package com.pangea.stores.activities

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.pangea.stores.R
import com.pangea.stores.StoreApplication
import com.pangea.stores.adapters.StoreAdapter
import com.pangea.stores.databinding.ActivityMainBinding
import com.pangea.stores.interfaces.OnClickListener
import com.pangea.stores.models.StoreEntity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.pangea.stores.activities.fragments.EditStoreFragment
import com.pangea.stores.interfaces.MainAux
import com.pangea.stores.interfaces.StoreDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.net.toUri


class MainActivity : AppCompatActivity(), OnClickListener, MainAux {

    private lateinit var mBinding: ActivityMainBinding
    private lateinit var mAdapter: StoreAdapter
    private lateinit var mGridLayout: GridLayoutManager
    private lateinit var mStoreDao: StoreDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        mStoreDao = (application as StoreApplication).database.storeDao()
        mBinding.fab.setOnClickListener {
            //Configuracion del fragment
            launchEditFragment()
        }

        //Configuracion del recyclerView
        setupRecyclerView()
    }

    //Configuracion del fragment
    private fun launchEditFragment(args: Bundle? = null) {
        val fragment = EditStoreFragment()
        if (args != null) fragment.arguments = args

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.add(R.id.main, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        hideFab()
    }

    //Configuracion del recyclerView
    private fun setupRecyclerView() {
        mAdapter = StoreAdapter(mutableListOf(), this)
        mGridLayout = GridLayoutManager(this, resources.getInteger(R.integer.main_columns))
        getStores()
        mBinding.recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = mGridLayout
            adapter = mAdapter
        }
    }


    //Obtener las tiendas
    private fun getStores() {
        lifecycleScope.launch(Dispatchers.IO) {
            val stores = mStoreDao.getAllStores()

            runOnUiThread {
                mAdapter.setStores(stores)
            }
        }
    }


    //Interface OnClickListener  -  Al hacer click en una tienda navega al fragment con el id de la tienda
    override fun onClick(storeId: Long) {
        val args = Bundle()
        args.putLong(getString(R.string.arg_id), storeId)

        launchEditFragment(args)

    }

    //Interface OnClickListener  -  Al hacer click en corazon de favorite agrega la tienda como favorite
    override fun onFavoriteStore(storeEntity: StoreEntity) {
        storeEntity.isFavorite = !storeEntity.isFavorite

        lifecycleScope.launch(Dispatchers.IO) {
            mStoreDao.updateStore(storeEntity)

            runOnUiThread {
                updateStore(storeEntity)
            }
        }

    }


    //Interface OnClickListener  -  Elimina la tienda
    override fun onDeleteStore(storeEntity: StoreEntity) {
        val items = resources.getStringArray(R.array.array_options_item)
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_options_title)
            .setItems(items) { dialogInterface, i ->
                when (i) {
                    0 -> confirmDelete(storeEntity)

                    1 -> dial(storeEntity.phone)

                    2 -> goToWebsite(storeEntity.website)
                }
            }
            .show()
    }

    //Eliminar una tienda
    private fun confirmDelete(storeEntity: StoreEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_delete_title)
            .setPositiveButton(R.string.dialog_delete_confirm) { dialogInterface, i ->
                lifecycleScope.launch(Dispatchers.IO) {
                    mStoreDao.deleteStore(storeEntity)

                    runOnUiThread {
                        mAdapter.delete(storeEntity)
                    }
                }

            }
            .setNegativeButton(R.string.dialog_delete_cancel, null)
            .show()
    }

    //Llamar a la tienda
    private fun dial(phone: String) {
        val callIntent = Intent().apply {
            action = Intent.ACTION_DIAL
            data = "tel:$phone".toUri()
        }
        startIntent(callIntent)
    }


    //Ir al sitio web de la tienda
    private fun goToWebsite(website: String) {
        if (website.isEmpty()) {
            Toast.makeText(this, R.string.main_error_website, Toast.LENGTH_SHORT).show()

        } else {
            val websiteIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = website.toUri()
            }
            startIntent(websiteIntent)

        }
    }

    //Validar que sea una aplicacion valida (ejemplo Llamar o ir al sitio web)
    private fun startIntent(intent: Intent){
        if (intent.resolveActivity(packageManager) != null)
            startActivity(intent)
        else
            Toast.makeText(this, getString(R.string.no_compatible_app_found), Toast.LENGTH_SHORT)
                .show()
    }


    //Interface MainAux  - visibilidad del boton flotante
    override fun hideFab(isVisible: Boolean) {
        if (isVisible) mBinding.fab.show() else mBinding.fab.hide()

    }

    //Interface MainAux  - Crea la tienda
    override fun addStore(storeEntity: StoreEntity) {
        mAdapter.add(storeEntity)

    }

    //Interface MainAux  - Actualiza la tienda
    override fun updateStore(storeEntity: StoreEntity) {
        mAdapter.update(storeEntity)

    }
}