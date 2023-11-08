package ru.practicum.android.diploma.filter.ui.fragment

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.addCallback
import androidx.annotation.ColorRes
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import com.fondesa.kpermissions.allGranted
import com.fondesa.kpermissions.extension.permissionsBuilder
import com.fondesa.kpermissions.extension.send
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.custom_view.ButtonWithSelectedValues
import ru.practicum.android.diploma.common.custom_view.model.ButtonWithSelectedValuesLocationState
import ru.practicum.android.diploma.common.custom_view.model.ButtonWithSelectedValuesTextState
import ru.practicum.android.diploma.databinding.FragmentFilteringSettingsBinding
import ru.practicum.android.diploma.filter.ui.model.ButtonState
import ru.practicum.android.diploma.filter.ui.model.ClearFieldButtonNavigationState
import ru.practicum.android.diploma.filter.ui.model.DialogState
import ru.practicum.android.diploma.filter.ui.model.FilterFieldsState
import ru.practicum.android.diploma.filter.ui.model.LocationState
import ru.practicum.android.diploma.filter.ui.model.FilterSettingsState
import ru.practicum.android.diploma.filter.ui.viewModel.FilteringSettingsViewModel

class FilteringSettingsFragment : Fragment() {

    private var _binding: FragmentFilteringSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModel<FilteringSettingsViewModel>()

    private var salary = BLANK_STRING

    private var confirmDialog: MaterialAlertDialogBuilder? = null


    // FusedLocationProviderClient - Main class for receiving location updates.
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // This will store current location info
    private var currentLocation: Location? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilteringSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initOnClicks()

        initObservers()

        initListeners()

        initConfirmDialog()

        viewModel.updateStates()

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initOnClicks() {
        binding.areaCustomView.onButtonClick {
            viewModel.areaButtonClicked()
            binding.selectedEnterTheAmountTextInputEditText.clearFocus()
        }

        binding.areaCustomView.setOnClickListener {
            viewModel.onAreaFieldClicked()
        }

        binding.industryCustomView.onButtonClick {
            viewModel.industryButtonClicked()
            binding.selectedEnterTheAmountTextInputEditText.clearFocus()
        }

        binding.industryCustomView.setOnClickListener {
            viewModel.onIndustryFieldClicked()
        }

        binding.filteringSettingsToolbar.setNavigationOnClickListener {
            viewModel.backButtonClicked()
        }

        binding.enterTheAmountTextInputLayout.setEndIconOnClickListener {
            viewModel.clearSalaryButtonClicked()
            binding.selectedEnterTheAmountTextInputEditText.clearFocus()
            setTextInputLayoutHintColor(
                binding.enterTheAmountTextInputLayout, requireContext(), R.color.gray_white
            )
        }

        binding.filteringSettingsOnlyWithSalaryCheckbox.setOnClickListener {
            viewModel.setOnlyWithSalary(
                binding.filteringSettingsOnlyWithSalaryCheckbox.isChecked
            )
            binding.selectedEnterTheAmountTextInputEditText.clearFocus()
        }

        binding.resetButton.setOnClickListener {
            viewModel.resetButtonClicked()
            binding.selectedEnterTheAmountTextInputEditText.clearFocus()
        }

        binding.applyButton.setOnClickListener {
            viewModel.applyButtonClicked()

            val bundle = Bundle()
            bundle.putBoolean(IS_SEARCH_WITH_NEW_FILTER_NEED, true)
            setFragmentResult(IS_SEARCH_WITH_NEW_FILTER_NEED, bundle)
            findNavController().popBackStack()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            viewModel.backButtonClicked()
        }

        binding.areaCustomView.onLocationButtonClick {
            permissionsBuilder(Manifest.permission.ACCESS_FINE_LOCATION).build().send {
                if (it.allGranted()) {
                    viewModel.getLocation()
                    getLocation()
                } else viewModel.locationAccessDenied()
            }
        }
    }

    private fun initObservers() {
        viewModel.observeStateLiveData().observe(viewLifecycleOwner) {
            render(it)
        }
    }

    private fun render(state: FilterSettingsState) {
        when (state) {
            is FilterSettingsState.Content -> {
                renderButtonWithSelectedValues(
                    state.areaField, binding.areaCustomView, resources.getString(R.string.workplace)
                )
                renderButtonWithSelectedValues(
                    state.industryField,
                    binding.industryCustomView,
                    resources.getString(R.string.industry)
                )

                if (state.isItInitSalaryField) {
                    binding.selectedEnterTheAmountTextInputEditText.setText(state.salaryField)
                }

                if (state.isItInitOnlySalary) {
                    binding.filteringSettingsOnlyWithSalaryCheckbox.isChecked = state.onlyWithSalary
                }

                binding.applyButton.isVisible = state.isDataChanged

                binding.resetButton.isVisible = state.isDataChanged
            }

            FilterSettingsState.Navigate.NavigateBackWithResult -> {
                val bundle = Bundle()
                bundle.putBoolean(IS_FILTER_CHANGED, true)
                setFragmentResult(IS_FILTER_CHANGED, bundle)
                findNavController().popBackStack()
            }

            FilterSettingsState.Navigate.NavigateBackWithoutResult -> {
                findNavController().popBackStack()
            }
        viewModel.observeDialogState().observe(viewLifecycleOwner) {
            renderDialogState(it)
        }

        viewModel.observeLocationState().observe(viewLifecycleOwner) {
            renderLocationState(it)

            FilterSettingsState.Navigate.NavigateToChoosingIndustry -> {
                findNavController().navigate(R.id.action_filteringSettingsFragment_to_filteringSectorFragment)
            }

            FilterSettingsState.Navigate.NavigateToChoosingWorkplace -> {
                findNavController().navigate(R.id.action_filteringSettingsFragment_to_filteringChoosingWorkplaceFragment)
            }
        }
    }

    private fun initListeners() {
        binding.selectedEnterTheAmountTextInputEditText.doOnTextChanged { input, _, _, _ ->
            binding.enterTheAmountTextInputLayout.apply {
                if (!input.isNullOrBlank()) {
                    viewModel.setSalaryAmount(input.toString())
                }
                if (input != null && input.isBlank()) {
                    viewModel.clearSalary()
                }
                setHintColor(input.toString())
            }
        }

        binding.filteringSettingsOnlyWithSalaryCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.selectedEnterTheAmountTextInputEditText.clearFocus()
            viewModel.setOnlyWithSalary(isChecked)
        }
    }
    private fun renderButtonWithSelectedValues(
        text: String, customView: ButtonWithSelectedValues, hint: String
    ) {
        if (text.isNotBlank()) {
            customView.render(
                ButtonWithSelectedValuesState.Content(
                    text = text, hint = hint
                )

    private fun initConfirmDialog() {
        confirmDialog = MaterialAlertDialogBuilder(
            requireContext()
        ).setTitle(R.string.location_title).setMessage(
            R.string.location_permission
        )
            .setNegativeButton(R.string.location_denied) { _, _ ->

            }.setPositiveButton(R.string.location_procced) { _, _ ->
                viewModel.openAppsSettings()
            }
    }

    private fun renderButtonWithSelectedValues(
        state: FilterFieldsState, customView: ButtonWithSelectedValues, hint: String
    ) {
        when (state) {
            is FilterFieldsState.Content -> customView.renderTextState(
                ButtonWithSelectedValuesTextState.Content(
                    text = state.text, hint = hint
                )
            )

            FilterFieldsState.Empty -> customView.renderTextState(
                ButtonWithSelectedValuesTextState.Empty(
                    hint = hint
                )
            )
        }
    }

    private fun renderClearAreaButtonNavigation(state: ClearFieldButtonNavigationState) {
        viewModel.updateButtonsStates()
        when (state) {
            ClearFieldButtonNavigationState.ClearField -> viewModel.clearArea()
            ClearFieldButtonNavigationState.Navigate -> {
                findNavController().navigate(R.id.action_filteringSettingsFragment_to_filteringChoosingWorkplaceFragment)
            }
        }
    }

    private fun renderClearIndustryButtonNavigation(state: ClearFieldButtonNavigationState) {
        viewModel.updateButtonsStates()
        when (state) {
            ClearFieldButtonNavigationState.ClearField -> viewModel.clearIndustry()
            ClearFieldButtonNavigationState.Navigate -> {
                findNavController().navigate(R.id.action_filteringSettingsFragment_to_filteringSectorFragment)
            }
        }
    }

    private fun renderButtonState(state: ButtonState, button: Button) {
        when (state) {
            ButtonState.Gone -> button.isVisible = false
            ButtonState.Visible -> button.isVisible = true
        }
    }

    private fun renderDialogState(state: DialogState) {
        when (state) {
            DialogState.ShowDialog -> confirmDialog?.show()
        }
    }

    private fun setFragmentResult() {
        val bundle = Bundle()
        bundle.putBoolean(IS_SEARCH_WITH_NEW_FILTER_NEED, true)
        setFragmentResult(IS_SEARCH_WITH_NEW_FILTER_NEED, bundle)
    }

    private fun renderLocationState(state: LocationState) {
        when (state) {
            LocationState.Empty -> binding.areaCustomView.renderLocationState(
                ButtonWithSelectedValuesLocationState.LocationEmpty
            )

            LocationState.Error -> showLocationError()
            LocationState.Loading -> binding.areaCustomView.renderLocationState(
                ButtonWithSelectedValuesLocationState.LocationLoading
            )
        } else {
            customView.render(
                ButtonWithSelectedValuesState.Empty(hint = hint)
            )
        }
    }

            LocationState.Success -> binding.areaCustomView.renderLocationState(
                ButtonWithSelectedValuesLocationState.LocationSuccess
            )
        }
    }

    private fun setHintColor(text: String) {
        binding.selectedEnterTheAmountTextInputEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                setTextInputLayoutHintColor(
                    binding.enterTheAmountTextInputLayout, requireContext(), R.color.blue
                )
            } else {
                if (text.isBlank()) {
                    setTextInputLayoutHintColor(
                        binding.enterTheAmountTextInputLayout, requireContext(), R.color.gray_white
                    )
                } else {
                    setTextInputLayoutHintColor(
                        binding.enterTheAmountTextInputLayout,
                        requireContext(),
                        R.color.black_universal
                    )
                }
            }
    private fun showLocationError() {
        Toast.makeText(requireContext(), R.string.location_error, Toast.LENGTH_LONG).show()
        binding.areaCustomView.renderLocationState(ButtonWithSelectedValuesLocationState.LocationEmpty)
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (isLocationEnabled()) {
            fusedLocationProviderClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                val location: Location? = task.result
                if (location == null) {
                    Log.d("MY_TAG", "location == null")

                    requestNewLocationData()
                } else {
                    Log.d("MY_TAG", "location != null")
                    currentLocation = location
                    Log.d(
                        "MY_TAG",
                        "latitude = ${currentLocation!!.latitude}, longitude= ${currentLocation!!.longitude}"
                    )
                    val coordinates =
                        "${currentLocation!!.longitude}, ${currentLocation!!.latitude}"
                    viewModel.getFilterFromGeocoder(coordinates)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val mLocationRequest =
            LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = 0
                fastestInterval = 0
                numUpdates = 1
            }

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        Log.d("MY_TAG", "getFusedLocationProviderClient")


        fusedLocationProviderClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback, Looper.myLooper()
        )
        Log.d("MY_TAG", "requestLocationUpdates")

    }

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val mLastLocation: Location? = locationResult.lastLocation
            currentLocation = mLastLocation
        }
    }

    private fun setTextInputLayoutHintColor(
        textInputLayout: TextInputLayout, context: Context, @ColorRes colorIdRes: Int
    ) {
        textInputLayout.defaultHintTextColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorIdRes))
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun setTextInputLayoutHintColor(
        textInputLayout: TextInputLayout, context: Context, @ColorRes colorIdRes: Int
    ) {
        textInputLayout.defaultHintTextColor =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorIdRes))
    }

    companion object {
        private const val IS_FILTER_CHANGED = "Is filter changed"
    }
}