import re

file_path = r'E:\tbterminalapp\app\src\main\java\com\tbterminal\app\MainActivity.kt'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

# 1. Extract AdminReceivable block to use as a template
receivable_start = text.find('composable(AppRoute.Receivables.route)')
brace_count = 0
in_block = False
receivable_end = -1
for i in range(receivable_start, len(text)):
    if text[i] == '{':
        brace_count += 1
        in_block = True
    elif text[i] == '}':
        brace_count -= 1
    if in_block and brace_count == 0:
        receivable_end = i
        break
receivable_block = text[receivable_start:receivable_end+1]

# Create AdminTransactionHistory block from template
history_block = receivable_block.replace('AppRoute.Receivables.route', 'AppRoute.SalesTransactions.route')
history_block = history_block.replace('AdminReceivableScreen', 'AdminTransactionHistoryScreen')
history_block = history_block.replace('receivableRepository = appContainer.receivableRepository,', 'cashReconciliationRepository = appContainer.cashReconciliationRepository,\n                                    onReceiptClick = { transactionId ->\n                                        navController.navigate(AppRoute.AdminReceiptDetail.createRoute(transactionId)) { launchSingleTop = true }\n                                    },')
history_block = history_block.replace('AdminDestination.Receivables', 'AdminDestination.SalesTransactions')

# Create AdminReceiptDetail block from template
receipt_block = receivable_block.replace('composable(AppRoute.Receivables.route) {', 'composable(\n                            route = AppRoute.AdminReceiptDetail.route,\n                            arguments = listOf(navArgument("transactionId") { type = NavType.StringType })\n                        ) { backStackEntry ->\n                            val transactionId = backStackEntry.arguments?.getString("transactionId") ?: ""')
receipt_block = receipt_block.replace('AdminReceivableScreen', 'AdminReceiptDetailScreen')
receipt_block = receipt_block.replace('receivableRepository = appContainer.receivableRepository,', 'transactionId = transactionId,\n                                    cashReconciliationRepository = appContainer.cashReconciliationRepository,\n                                    onBackClick = { navController.popBackStack() },')
receipt_block = receipt_block.replace('AdminDestination.Receivables', 'AdminDestination.SalesTransactions')

# 2. Extract SalesTransactions block to replace
sales_start = text.find('composable(AppRoute.SalesTransactions.route)')
brace_count = 0
in_block = False
sales_end = -1
for i in range(sales_start, len(text)):
    if text[i] == '{':
        brace_count += 1
        in_block = True
    elif text[i] == '}':
        brace_count -= 1
    if in_block and brace_count == 0:
        sales_end = i
        break

# Replace old SalesTransactions with history_block + receipt_block
new_text = text[:sales_start] + history_block + '\n\n                        ' + receipt_block + text[sales_end+1:]

# Add AppRoute.AdminReceiptDetail to sealed interface
if 'data object AdminReceiptDetail' not in new_text:
    interface_marker = 'sealed interface AppRoute {'
    idx = new_text.find(interface_marker)
    if idx != -1:
        insert_idx = new_text.find('\n', idx)
        route_str = '\n    data object AdminReceiptDetail : AppRoute {\n        override val route = "admin/sales/receipt/{transactionId}"\n        fun createRoute(transactionId: String) = "admin/sales/receipt/$transactionId"\n    }'
        new_text = new_text[:insert_idx] + route_str + new_text[insert_idx:]

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(new_text)

print('Updated MainActivity.kt successfully!')
