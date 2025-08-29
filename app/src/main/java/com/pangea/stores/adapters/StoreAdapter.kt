package com.pangea.stores.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.pangea.stores.R
import com.pangea.stores.databinding.ItemStoreBinding
import com.pangea.stores.interfaces.OnClickListener
import com.pangea.stores.models.StoreEntity
import kotlinx.coroutines.Dispatchers

class StoreAdapter(private var stores: MutableList<StoreEntity>, private var listener: OnClickListener):
    RecyclerView.Adapter<StoreAdapter.ViewHolder>() {

        private lateinit var mContext: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        mContext = parent.context
        val view = LayoutInflater.from(mContext).inflate(R.layout.item_store, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val store = stores[position]
        with(holder){
            setListener(store)
            binding.tvName.text = store.name
            binding.cbFavorite.isChecked = store.isFavorite

            Glide.with(mContext)
                .load(store.photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.imgPhoto)
        }

    }

    override fun getItemCount(): Int = stores.size

    //Crea la tienda
    fun add(storeEntity: StoreEntity){
        if (!stores.contains(storeEntity)){
            stores.add(storeEntity)
            notifyItemInserted(stores.lastIndex)
        }
    }

    //Obtener las tiendas
    fun setStores(stores: MutableList<StoreEntity>) {
        this.stores = stores
        notifyDataSetChanged()
    }

    //Actualiza la tienda
    fun update(storeEntity: StoreEntity){
        val index = stores.indexOfFirst { it.id == storeEntity.id }
        if(index != -1){
            stores[index] = storeEntity
            notifyItemChanged(index)
        }
    }

    //Elimina la tienda
    fun delete(storeEntity: StoreEntity){
        val index = stores.indexOf(storeEntity)
        if(index != -1){
            stores.removeAt(index)
            notifyItemRemoved(index)
        }

    }


    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view){
        val binding = ItemStoreBinding.bind(view)

        fun setListener(storeEntity: StoreEntity){

            //Al hacer click en una tienda navega al fragment con el id de la tienda
            binding.root.setOnClickListener {
                listener.onClick(storeEntity.id)
            }

            //Al hacer click en corazon de favorite agrega la tienda como favorite
            binding.cbFavorite.setOnClickListener {
                listener.onFavoriteStore(storeEntity)
            }

            //Al hacer click prolongado en una tienda la elimina
            binding.root.setOnLongClickListener {
                listener.onDeleteStore(storeEntity)
                true
            }
        }


    }
}