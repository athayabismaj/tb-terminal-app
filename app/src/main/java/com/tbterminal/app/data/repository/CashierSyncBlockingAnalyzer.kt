package com.tbterminal.app.data.repository

import com.tbterminal.app.data.local.model.SyncEntityType
import com.tbterminal.app.data.local.model.SyncOperation
import com.tbterminal.app.data.local.model.SyncStatus
import com.tbterminal.app.data.sync.CashSessionActiveBlockingMatcher
import com.tbterminal.app.data.sync.OfflineTimeConflictPolicy

internal data class SyncBlockingNode(
    val queueId: Long,
    val entityType: SyncEntityType,
    val entityLocalId: Long,
    val operation: SyncOperation,
    val status: SyncStatus,
    val lastErrorCode: String?,
    val sessionLocalId: Long?,
    val sessionServerId: String?,
    val sessionOpenedAtMillis: Long?,
    val createdAtMillis: Long,
    val displayLabel: String,
)

internal data class SyncBlockingInfo(
    val blockingCount: Int = 0,
    val blockedByRootQueueId: Long? = null,
    val blockedByRootLabel: String? = null,
)

/**
 * Computes transitive blocking relationships across unsynced cashier queue items.
 *
 * Only items that are currently `PENDING` and transitively held back by a stalled root item
 * (`CONFLICT`, `FAILED`, future-clock deferral, or an unclosed blocking session) receive
 * `blockedByRoot*` attribution; the root item receives `blockingCount` of affected items.
 */
internal object CashierSyncBlockingAnalyzer {
    fun analyze(nodes: List<SyncBlockingNode>): Map<Long, SyncBlockingInfo> {
        if (nodes.isEmpty()) return emptyMap()

        val nodeById = nodes.associateBy { it.queueId }
        val openNodeBySession = mutableMapOf<Long, SyncBlockingNode>()
        val closeNodeBySession = mutableMapOf<Long, SyncBlockingNode>()
        val childNodesBySession = mutableMapOf<Long, MutableList<SyncBlockingNode>>()

        nodes.forEach { node ->
            val sessionId = node.sessionLocalId ?: return@forEach
            when (node.entityType) {
                SyncEntityType.CASH_SESSION -> {
                    when (node.operation) {
                        SyncOperation.CREATE -> openNodeBySession.putIfAbsent(sessionId, node)
                        SyncOperation.UPDATE -> closeNodeBySession.putIfAbsent(sessionId, node)
                        SyncOperation.DELETE -> Unit
                    }
                }
                SyncEntityType.TRANSACTION,
                SyncEntityType.CASH_EXPENSE -> {
                    childNodesBySession.getOrPut(sessionId) { mutableListOf() }.add(node)
                }
                SyncEntityType.UNSUPPORTED -> Unit
            }
        }

        val orderedSessionIds = nodes.mapNotNull { node ->
            node.sessionLocalId?.let { sid ->
                sid to (node.sessionOpenedAtMillis ?: Long.MAX_VALUE)
            }
        }
            .distinctBy { it.first }
            .sortedWith(compareBy<Pair<Long, Long>>({ it.second }, { it.first }))
            .map { it.first }

        val directBlocker = mutableMapOf<Long, Long>()

        nodes.forEach { node ->
            if (node.status != SyncStatus.PENDING) return@forEach
            val sessionId = node.sessionLocalId ?: return@forEach
            when (node.entityType) {
                SyncEntityType.TRANSACTION,
                SyncEntityType.CASH_EXPENSE -> {
                    openNodeBySession[sessionId]?.let { openNode ->
                        if (openNode.queueId != node.queueId) {
                            directBlocker[node.queueId] = openNode.queueId
                        }
                    }
                }
                SyncEntityType.CASH_SESSION -> {
                    when (node.operation) {
                        SyncOperation.UPDATE -> {
                            val openNode = openNodeBySession[sessionId]
                            if (openNode != null && openNode.queueId != node.queueId) {
                                directBlocker[node.queueId] = openNode.queueId
                            } else {
                                val children = childNodesBySession[sessionId].orEmpty()
                                selectPrimaryBlockingChild(children)?.let { child ->
                                    if (child.queueId != node.queueId) {
                                        directBlocker[node.queueId] = child.queueId
                                    }
                                }
                            }
                        }
                        SyncOperation.CREATE -> {
                            val sessionIndex = orderedSessionIds.indexOf(sessionId)
                            if (sessionIndex > 0) {
                                val predecessor = orderedSessionIds.subList(0, sessionIndex)
                                    .firstNotNullOfOrNull { prevSessionId ->
                                        closeNodeBySession[prevSessionId]
                                            ?: openNodeBySession[prevSessionId]
                                            ?: selectPrimaryBlockingChild(childNodesBySession[prevSessionId].orEmpty())
                                    }
                                if (predecessor != null && predecessor.queueId != node.queueId) {
                                    directBlocker[node.queueId] = predecessor.queueId
                                }
                            }
                        }
                        SyncOperation.DELETE -> Unit
                    }
                }
                SyncEntityType.UNSUPPORTED -> Unit
            }
        }

        fun isStalledRoot(node: SyncBlockingNode): Boolean {
            if (node.status == SyncStatus.CONFLICT || node.status == SyncStatus.FAILED) {
                return true
            }
            if (node.status == SyncStatus.PENDING &&
                OfflineTimeConflictPolicy.isOccurredAtInvalidCode(node.lastErrorCode)
            ) {
                return true
            }
            if (node.status == SyncStatus.PENDING &&
                node.lastErrorCode.equals(
                    CashSessionActiveBlockingMatcher.ERROR_CODE_ACTIVE_SESSION_BLOCKING_OPEN,
                    ignoreCase = true,
                ) &&
                directBlocker[node.queueId] == null
            ) {
                return true
            }
            return false
        }

        val rootForNode = mutableMapOf<Long, SyncBlockingNode>()
        nodes.forEach { node ->
            if (node.status != SyncStatus.PENDING) return@forEach
            var currentId = directBlocker[node.queueId] ?: return@forEach
            val visited = mutableSetOf(node.queueId)
            while (true) {
                if (!visited.add(currentId)) break
                val nextId = directBlocker[currentId]
                if (nextId == null) {
                    val root = nodeById[currentId]
                    if (root != null && root.queueId != node.queueId && isStalledRoot(root)) {
                        rootForNode[node.queueId] = root
                    }
                    break
                }
                currentId = nextId
            }
        }

        val blockingCounts = mutableMapOf<Long, Int>()
        rootForNode.values.forEach { root ->
            blockingCounts[root.queueId] = (blockingCounts[root.queueId] ?: 0) + 1
        }

        return nodes.associate { node ->
            val root = rootForNode[node.queueId]
            node.queueId to SyncBlockingInfo(
                blockingCount = blockingCounts[node.queueId] ?: 0,
                blockedByRootQueueId = root?.queueId,
                blockedByRootLabel = root?.displayLabel,
            )
        }
    }

    private fun selectPrimaryBlockingChild(children: List<SyncBlockingNode>): SyncBlockingNode? {
        if (children.isEmpty()) return null
        return children.minWithOrNull(
            compareBy<SyncBlockingNode>(
                { childPriority(it) },
                { it.createdAtMillis },
                { it.queueId },
            )
        )
    }

    private fun childPriority(node: SyncBlockingNode): Int = when {
        node.status == SyncStatus.CONFLICT -> 0
        node.status == SyncStatus.FAILED -> 1
        node.status == SyncStatus.PENDING &&
            OfflineTimeConflictPolicy.isOccurredAtInvalidCode(node.lastErrorCode) -> 2
        else -> 3
    }
}
