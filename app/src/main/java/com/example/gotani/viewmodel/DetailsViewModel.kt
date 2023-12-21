package com.example.gotani.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.gotani.data.CartProduct
import com.example.gotani.firebase.FirebaseCommon
import com.example.gotani.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class DetailsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val firebaseCommon: FirebaseCommon
) : ViewModel() {

    private val _addToCart = MutableStateFlow<Resource<CartProduct>>(Resource.Unspecified())
    val addToCart = _addToCart.asStateFlow()

    fun addUpdateProductInCart(cartProduct: CartProduct) {
        viewModelScope.launch {
            _addToCart.emit(Resource.Loading())
            try {
                val querySnapshot = firestore.collection("user")
                    .document(auth.uid!!)
                    .collection("cart")
                    .whereEqualTo("product.id", cartProduct.product.id)
                    .get()
                    .await()

                val documents = querySnapshot.documents

                if (documents.isEmpty()) {
                    addNewProduct(cartProduct)
                } else {
                    val product = documents.first().toObject(CartProduct::class.java)
                    if (product != null && product.product == cartProduct.product &&
                        product.selectedColor == cartProduct.selectedColor &&
                        product.selectedSize == cartProduct.selectedSize
                    ) {
                        val documentId = documents.first().id
                        increaseQuantity(documentId, cartProduct)
                    } else {
                        addNewProduct(cartProduct)
                    }
                }
            } catch (e: Exception) {
                _addToCart.emit(Resource.Error(e.message.toString()))
            }
        }
    }

    private fun addNewProduct(cartProduct: CartProduct) {
        firebaseCommon.addProductToCart(cartProduct) { addedProduct, e ->
            viewModelScope.launch {
                if (e == null)
                    _addToCart.emit(Resource.Success(addedProduct!!))
                else
                    _addToCart.emit(Resource.Error(e.message.toString()))
            }
        }
    }

    private fun increaseQuantity(documentId: String, cartProduct: CartProduct) {
        firebaseCommon.increaseQuantity(documentId) { _, e ->
            viewModelScope.launch {
                if (e == null)
                    _addToCart.emit(Resource.Success(cartProduct))
                else
                    _addToCart.emit(Resource.Error(e.message.toString()))
            }
        }
    }
}
