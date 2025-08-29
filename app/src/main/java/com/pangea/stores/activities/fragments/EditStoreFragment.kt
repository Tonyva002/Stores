package com.pangea.stores.activities.fragments

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Patterns
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.MenuProvider
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.pangea.stores.R
import com.pangea.stores.StoreApplication
import com.pangea.stores.activities.MainActivity
import com.pangea.stores.databinding.FragmentEditStoreBinding
import com.pangea.stores.interfaces.StoreDao
import com.pangea.stores.models.StoreEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class EditStoreFragment : Fragment() {

    private lateinit var mBinding: FragmentEditStoreBinding
    private var mActivity: MainActivity? = null
    private var mPhotoUrlJob: Job? = null
    private var mIsEditMode: Boolean = false
    private var mStoreEntity: StoreEntity? = null
    private lateinit var mStoreDao: StoreDao

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        //Inflar el xml
        mBinding = FragmentEditStoreBinding.inflate(inflater, container, false)
        return mBinding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mStoreDao = (requireActivity().application as StoreApplication).database.storeDao()

        //Recuperar el id de la tienda enviado desde el activity
        val id = arguments?.getLong(getString(R.string.arg_id), 0)
        if (id != null && id != 0L) {
            mIsEditMode = true
            getStoreById(id)
        } else {
            mIsEditMode = false
            mStoreEntity = StoreEntity(name = "", phone = "", photoUrl = "")
        }
        setupActionBar()
        setupTextFields()


        //Configurar el menu actionBar
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_save, menu)
            }


            //Funcionalidades del menu del actionBar
            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    android.R.id.home -> {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                        true
                    }

                    R.id.action_save -> {
                        if (mStoreEntity != null && validateFields(
                                mBinding.tilPhotoUrl,
                                mBinding.tilPhone,
                                mBinding.tilName
                            )
                        ) createStore()
                        true
                    }

                    else -> false
                }
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true){
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.dialog_exit_title)
                    .setMessage(R.string.dialog_exit_message)
                    .setPositiveButton(R.string.dialog_exit_ok){dialogInterface, i ->
                        if (isEnabled){
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }

                    }
                    .setNegativeButton(R.string.dialog_delete_cancel, null)
                    .show()


            }
        })

    }

    //Configurar el actionBar
    private fun setupActionBar() {
        mActivity = activity as? MainActivity
        mActivity?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = if (mIsEditMode) getString(R.string.edit_store)
            else getString(R.string.create_store)

        }
    }

    //Valida los campos requeridos y formato de la url
    private fun setupTextFields() {
        with(mBinding) {
            etName.addTextChangedListener { validateFields(tilName) }
            etPhone.addTextChangedListener { validateFields(tilPhone) }

            etPhotoUrl.addTextChangedListener { editable ->
                val url = editable.toString().trim()

                val isValidUrl = validateUrlField(url, tilPhotoUrl, etPhotoUrl)

                mPhotoUrlJob?.cancel()

                // Esperar 200 ms antes de cargar la imagen
                mPhotoUrlJob = lifecycleScope.launch {
                    delay(200)
                    if (isValidUrl) {
                        loadImage(url)
                    } else {
                        imgPhoto.setImageResource(R.drawable.ic_image)
                    }
                }
            }

        }
    }


    //Cargar imagen con Glide
    private fun loadImage(url: String) {
        Glide.with(requireContext())
            .load(url)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .into(mBinding.imgPhoto)

    }

    //Valida los campos vacíos del formulario
    private fun validateFields(vararg textFields: TextInputLayout): Boolean {
        var isValid = true
        for (textField in textFields) {
            if (textField.editText?.text.toString().trim().isEmpty()) {
                textField.error = getString(R.string.required)
                isValid = false
            } else textField.error = null
        }

        if (!isValid) Toast.makeText(mActivity, R.string.store_message_valid, Toast.LENGTH_LONG)
            .show()

        return isValid
    }

    // Valida el formato de la url
    private fun validateUrlField(
        url: String,
        textInputLayout: TextInputLayout,
        editText: EditText
    ): Boolean {
        return if (url.isNotEmpty() && !Patterns.WEB_URL.matcher(url).matches()) {
            textInputLayout.error = getString(R.string.invalid_url)
            editText.requestFocus()
            false
        } else {
            textInputLayout.error = null
            true
        }
    }


    //Traer la informacion de la base de datos por id
    private fun getStoreById(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            mStoreEntity = mStoreDao.getStoreById(id)

            withContext(Dispatchers.Main) {

                mStoreEntity?.let { setUiStore(it) } ?: return@withContext
            }
        }
    }

    //Llena los campos con la informacion que viene desde la base de datos
    private fun setUiStore(storeEntity: StoreEntity) {
        with(mBinding) {
            etName.text = storeEntity.name.editable()
            etPhone.text = storeEntity.phone.editable()
            etWebsite.text = storeEntity.website.editable()
            etPhotoUrl.text = storeEntity.photoUrl.editable()
        }
    }

    private fun String.editable(): Editable = Editable.Factory.getInstance().newEditable(this)


    //Crea la tienda
    private fun createStore() {
        val store = mStoreEntity ?: return

        val name = mBinding.etName.text.toString().trim()
        val phone = mBinding.etPhone.text.toString().trim()
        val website = mBinding.etWebsite.text.toString().trim()
        val photoUrl = mBinding.etPhotoUrl.text.toString().trim()

        // Validar el formato de la url
        val isWebsiteValid = validateUrlField(website, mBinding.tilWebsite, mBinding.etWebsite)
        val isPhotoUrlValid = validateUrlField(photoUrl, mBinding.tilPhotoUrl, mBinding.etPhotoUrl)

        // Si alguno falla, detenemos la función
        if (!isWebsiteValid || !isPhotoUrlValid) return


        with(store) {
            this.name = name
            this.phone = phone
            this.website = website
            this.photoUrl = photoUrl
        }

        lifecycleScope.launch(Dispatchers.IO) {
            if (mIsEditMode) {
                mStoreDao.updateStore(store)
            } else {
                val newId = mStoreDao.addStore(store)
                store.id = newId
            }

            withContext(Dispatchers.Main) {
                if (mIsEditMode) {
                    mActivity?.updateStore(store)
                    Toast.makeText(mActivity, R.string.store_updated_success, Toast.LENGTH_SHORT)
                        .show()
                } else {
                    mActivity?.addStore(store)
                    Toast.makeText(mActivity, R.string.store_created_success, Toast.LENGTH_SHORT)
                        .show()
                }
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }


    //Metodo para ocultar el teclado (usarlo si es necesario)
    private fun hidekeyboard() {
        val inm = mActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inm.hideSoftInputFromWindow(requireView().windowToken, 0)

    }


    override fun onDestroyView() {
        mActivity?.supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            title = getString(R.string.app_name)
        }
        mActivity?.hideFab(true)
        mActivity = null
        super.onDestroyView()
    }
}