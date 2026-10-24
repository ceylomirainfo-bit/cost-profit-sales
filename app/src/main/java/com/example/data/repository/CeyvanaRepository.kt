package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.OrderNoteDao
import com.example.data.local.ProductDao
import com.example.data.local.SaleDao
import com.example.data.model.OrderNoteEntity
import com.example.data.model.ProductEntity
import com.example.data.model.ProductVariantEntity
import com.example.data.model.ProductWithVariants
import com.example.data.model.SaleEntity
import com.example.data.model.SaleLineItemEntity
import com.example.data.model.SaleWithItems
import kotlinx.coroutines.flow.Flow

class CeyvanaRepository(
    private val database: AppDatabase
) {
    private val productDao: ProductDao = database.productDao()
    private val saleDao: SaleDao = database.saleDao()
    private val orderNoteDao: OrderNoteDao = database.orderNoteDao()

    val activeProducts: Flow<List<ProductWithVariants>> = productDao.getActiveProductsWithVariants()
    val allProducts: Flow<List<ProductWithVariants>> = productDao.getAllProductsWithVariants()

    val allSales: Flow<List<SaleWithItems>> = saleDao.getAllSales()
    val saleDates: Flow<List<String>> = saleDao.getDistinctSaleDates()

    val allOrderNotes: Flow<List<OrderNoteEntity>> = orderNoteDao.getAllOrderNotes()
    val orderDates: Flow<List<String>> = orderNoteDao.getDistinctOrderDates()

    suspend fun ensureCatalogueInitialized() {
        if (productDao.getProductCount() == 0) {
            AppDatabase.populateInitialCatalogue(database)
        }
    }

    fun getSalesForDate(date: String): Flow<List<SaleWithItems>> {
        return saleDao.getSalesByDate(date)
    }

    fun getOrderNotesForDate(date: String): Flow<List<OrderNoteEntity>> {
        return orderNoteDao.getOrderNotesByDate(date)
    }

    suspend fun addProductWithVariants(
        product: ProductEntity,
        variants: List<ProductVariantEntity>
    ): Long {
        val prodId = productDao.insertProduct(product)
        val linkedVariants = variants.map { it.copy(productId = prodId) }
        productDao.insertVariants(linkedVariants)
        return prodId
    }

    suspend fun updateProduct(product: ProductEntity) {
        productDao.updateProduct(product)
    }

    suspend fun updateProductAndVariants(
        product: ProductEntity,
        updatedVariants: List<ProductVariantEntity>
    ) {
        productDao.updateProduct(product)
        productDao.deleteVariantsByProductId(product.id)
        val linked = updatedVariants.map { it.copy(id = 0, productId = product.id) }
        productDao.insertVariants(linked)
    }

    /**
     * Checks if product has historical sales.
     * If yes, safe-archives to preserve historical integrity.
     * If no, permanently deletes.
     * Returns true if deleted, false if archived.
     */
    suspend fun deleteOrArchiveProduct(product: ProductEntity): Boolean {
        val historicalCount = productDao.countHistoricalSalesForProduct(product.id)
        return if (historicalCount > 0) {
            productDao.archiveProduct(product.id)
            false
        } else {
            productDao.deleteVariantsByProductId(product.id)
            productDao.deleteProduct(product)
            true
        }
    }

    suspend fun addVariant(variant: ProductVariantEntity): Long {
        return productDao.insertVariant(variant)
    }

    suspend fun updateVariant(variant: ProductVariantEntity) {
        productDao.updateVariant(variant)
    }

    suspend fun deleteVariant(variant: ProductVariantEntity) {
        productDao.deleteVariant(variant)
    }

    suspend fun recordSale(
        sale: SaleEntity,
        items: List<SaleLineItemEntity>
    ): Long {
        val saleId = saleDao.insertSale(sale)
        val linkedItems = items.map { it.copy(id = 0, saleId = saleId) }
        saleDao.insertSaleLineItems(linkedItems)
        return saleId
    }

    suspend fun updateSaleWithItems(
        sale: SaleEntity,
        items: List<SaleLineItemEntity>
    ) {
        saleDao.updateSale(sale)
        saleDao.deleteLineItemsBySaleId(sale.id)
        val linkedItems = items.map { it.copy(id = 0, saleId = sale.id) }
        saleDao.insertSaleLineItems(linkedItems)
    }

    suspend fun deleteSale(sale: SaleEntity) {
        saleDao.deleteSaleWithItems(sale)
    }

    suspend fun addOrderNote(note: OrderNoteEntity): Long {
        return orderNoteDao.insertOrderNote(note)
    }

    suspend fun updateOrderNote(note: OrderNoteEntity) {
        orderNoteDao.updateOrderNote(note)
    }

    suspend fun deleteOrderNote(note: OrderNoteEntity) {
        orderNoteDao.deleteOrderNote(note)
    }
}
