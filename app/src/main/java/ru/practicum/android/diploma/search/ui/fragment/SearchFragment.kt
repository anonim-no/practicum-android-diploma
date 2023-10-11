package ru.practicum.android.diploma.search.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.practicum.android.diploma.R
import ru.practicum.android.diploma.common.ui.model.VacancyUi
import ru.practicum.android.diploma.common.util.recycleView.RVAdapter
import ru.practicum.android.diploma.databinding.FragmentSearchBinding
import ru.practicum.android.diploma.search.ui.model.ErrorStatusUi
import ru.practicum.android.diploma.search.ui.model.SearchError
import ru.practicum.android.diploma.search.ui.model.SearchState
import ru.practicum.android.diploma.search.ui.model.TextWatcherJustOnTextChanged
import ru.practicum.android.diploma.search.ui.viewModel.SearchViewModel

class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModel()

    private var textWatcher: TextWatcher? = null
    private var vacanciesAdapter: RVAdapter? = null

    private var inputSearchText: String = DEFAULT_TEXT
    private var isClickAllowed = true

    private var inputMethodManager: InputMethodManager? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recycleViewInit()
        setOnClicksAndActions()
        setOnTextWatchersTextChangeListeners()

        viewModel.observeState().observe(viewLifecycleOwner) {
            renderSearchState(it)
        }

        viewModel.observeErrorToastState().observe(viewLifecycleOwner) {
            renderErrorState(it)
        }

        isClickAllowed = true
    }

    override fun onDestroy() {
        super.onDestroy()
        textWatcher?.let { binding.searchScreenEditText.removeTextChangedListener(it) }
        binding.searchScreenRecyclerView.adapter = null
        vacanciesAdapter = null
        _binding = null
    }

    private fun recycleViewInit() {
        vacanciesAdapter = RVAdapter { vacancy ->
            if (isClickDebounce()) {
                val direction =
                    SearchFragmentDirections.actionSearchFragmentToVacancyFragment(vacancy.id)
                findNavController().navigate(direction)
            }
        }

        binding.searchScreenRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.searchScreenRecyclerView.adapter = vacanciesAdapter
    }

    private fun renderSearchState(state: SearchState) {
        when (state) {
            is SearchState.Error -> showError(state.errorStatus)
            is SearchState.Loading.LoadingSearch -> showLoadingSearch()
            is SearchState.Loading.LoadingPages -> showLoadingPages()
            SearchState.Success.Empty -> showEmpty()
            is SearchState.Success.SearchContent -> showContent(state.vacancies, state.foundVacancy)
        }
    }

    private fun renderErrorState(state: SearchError) {
        when (state) {
            SearchError.ERROR_OCCURRED -> {
                showToast(resources.getString(R.string.failed_to_get_a_list_of_vacancies))
                binding.searchScreenPaginationProgressBar.isVisible = false
            }

            SearchError.NO_CONNECTION -> {
                showToast(resources.getString(R.string.no_internet))
                binding.searchScreenPaginationProgressBar.isVisible = false
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun showContent(vacancies: List<VacancyUi>, foundVacancies: Int) {
        emptyScreen()
        vacanciesAdapter?.notifyDataSetChanged()
        vacanciesAdapter?.items = vacancies
        binding.searchScreenRecyclerView.isVisible = true
        binding.searchScreenRecyclerView.scrollToPosition(0)
        binding.counterVacanciesTextView.text = resources.getQuantityString(
            R.plurals.vacancy_plural, foundVacancies, foundVacancies
        )
        binding.counterVacanciesTextView.isVisible = true
    }


    private fun showEmpty() {
        emptyScreen()
        binding.placeholderSearchVacanciesImageView.isVisible = true
    }

    private fun showLoadingSearch() {
        emptyScreen()
        vacanciesAdapter?.items = listOf()
        binding.searchScreenFirstLoadingProgressBar.isVisible = true
        inputMethodManager?.hideSoftInputFromWindow(
            binding.searchScreenEditText.windowToken, 0
        )
    }

    private fun showLoadingPages() {
        binding.searchScreenPaginationProgressBar.isVisible = true
    }

    private fun showError(errorStatus: ErrorStatusUi) {
        when (errorStatus) {
            ErrorStatusUi.NO_CONNECTION -> {
                emptyScreen()
                binding.searchScreenNoInternetPlaceholder.isVisible = true
            }

            ErrorStatusUi.ERROR_OCCURRED -> {
                emptyScreen()
                binding.searchScreenServerErrorPlaceholder.isVisible = true
            }

            ErrorStatusUi.NOTHING_FOUND -> {
                emptyScreen()
                binding.searchScreenNothingFoundPlaceholder.isVisible = true
                binding.counterVacanciesTextView.text =
                    resources.getString(R.string.there_are_no_such_vacancies)
                binding.counterVacanciesTextView.isVisible = true
            }
        }
    }


    private fun setOnClicksAndActions() {
        inputMethodManager =
            activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        inputMethodManager?.hideSoftInputFromWindow(
            binding.searchScreenEditText.windowToken, 0
        )

        binding.searchFormButton.setOnClickListener {
            binding.searchScreenEditText.setText(DEFAULT_TEXT)
        }


//        binding.searchScreenHeaderFilterImageView.setOnClickListener {
//            findNavController().navigate(R.id.action_searchFragment_to_filteringSettingsFragment)
//        }

        binding.searchScreenRecyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    val pos =
                        (binding.searchScreenRecyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    val itemsCount = vacanciesAdapter?.itemCount
                    if (itemsCount != null) {
                        if (pos >= itemsCount - 1) {
                            viewModel.onLastItemReached()
                        }
                    }
                }
            }
        })
    }

    private fun setOnTextWatchersTextChangeListeners() {

        textWatcher = object : TextWatcherJustOnTextChanged {

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                setSearchEditTextEndDrawable(s)
                inputSearchText = binding.searchScreenEditText.text.toString()

                if (s?.isEmpty() == true) {
                    vacanciesAdapter?.items = listOf()
                    viewModel.clearSearchInput()
                }
                viewModel.searchDebounced(
                    changedText = s?.toString() ?: ""
                )

            }
        }

        textWatcher?.let { binding.searchScreenEditText.addTextChangedListener(it) }
    }

    private fun isClickDebounce(): Boolean {
        val current = isClickAllowed
        if (isClickAllowed) {
            isClickAllowed = false

            viewLifecycleOwner.lifecycleScope.launch {
                delay(CLICK_DEBOUNCE_DELAY_MILLIS)
                isClickAllowed = true
            }
        }
        return current
    }

    private fun setSearchEditTextEndDrawable(charSequence: CharSequence?) {
        if (charSequence.isNullOrEmpty()) {
            binding.searchFormButton.setImageResource(R.drawable.ic_search)
        } else {
            binding.searchFormButton.setImageResource(R.drawable.ic_cross)
        }
    }

    private fun emptyScreen() {
        binding.counterVacanciesTextView.isVisible = false
        binding.searchScreenRecyclerView.isVisible = false
        binding.searchScreenFirstLoadingProgressBar.isVisible = false
        binding.searchScreenPaginationProgressBar.isVisible = false
        binding.placeholderSearchVacanciesImageView.isVisible = false
        binding.searchScreenNoInternetPlaceholder.isVisible = false
        binding.searchScreenNothingFoundPlaceholder.isVisible = false
        binding.searchScreenServerErrorPlaceholder.isVisible = false
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val DEFAULT_TEXT = ""
        private const val CLICK_DEBOUNCE_DELAY_MILLIS = 1000L
    }
}