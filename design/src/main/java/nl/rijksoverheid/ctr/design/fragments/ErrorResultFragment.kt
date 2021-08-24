package nl.rijksoverheid.ctr.design.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import nl.rijksoverheid.ctr.design.R
import nl.rijksoverheid.ctr.design.databinding.FragmentErrorResultBinding
import nl.rijksoverheid.ctr.shared.error.ErrorResultFragmentData
import nl.rijksoverheid.ctr.shared.ext.launchUrl

class ErrorResultFragment: Fragment(R.layout.fragment_error_result) {

    companion object {

        private const val EXTRA_DATA = "EXTRA_DATA"

        fun getBundle(data: ErrorResultFragmentData): Bundle {
            val bundle = Bundle()
            bundle.putSerializable(EXTRA_DATA, data)
            return bundle
        }
    }

    private val data by lazy { arguments?.getSerializable(EXTRA_DATA) as ErrorResultFragmentData }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentErrorResultBinding.bind(view)

        binding.title.text = data.title
        binding.description.setHtmlText(data.description, htmlLinksEnabled = true)

        data.urlData?.let { urlData ->
            binding.urlButton.visibility = View.VISIBLE
            binding.urlButton.text = urlData.urlButtonTitle
            binding.urlButton.setOnClickListener {
                urlData.urlButtonUrl.launchUrl(requireActivity())
            }
        }

        binding.bottom.setButtonClick {
            data.buttonCallback.invoke()
        }
        binding.bottom.setButtonText(data.buttonTitle)
    }
}