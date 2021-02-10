package nl.rijksoverheid.ctr.holder.myoverview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.findNavController
import nl.rijksoverheid.ctr.holder.databinding.FragmentMyOverviewBinding
import nl.rijksoverheid.ctr.holder.digid.DigiDFragment
import nl.rijksoverheid.ctr.shared.ext.observeResult
import org.koin.android.viewmodel.ext.android.viewModel
import org.threeten.bp.OffsetDateTime
import timber.log.Timber

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class MyOverviewFragment : DigiDFragment() {

    private lateinit var binding: FragmentMyOverviewBinding
    private val qrCodeViewModel: QrCodeViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMyOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.noQr.card2Button.setOnClickListener {
            findNavController().navigate(MyOverviewFragmentDirections.actionChooseProvider())
        }

        observeResult(qrCodeViewModel.qrCodeLiveData, {
            Timber.v("IK KOM HIER #1")
            binding.noQr.root.visibility = View.GONE
            binding.existingQr.root.visibility = View.VISIBLE
        }, {
            Timber.v("IK KOM HIER #2")
            binding.existingQr.cardQrImage.setImageBitmap(it)
        }, {
            binding.noQr.root.visibility = View.VISIBLE
            binding.existingQr.root.visibility = View.GONE
            presentError()
        })

        binding.existingQr.cardQrImage.doOnPreDraw {
            observeResult(qrCodeViewModel.localTestResultLiveData, {
            }, { localTestResult ->
                if (localTestResult != null) {
                    qrCodeViewModel.generateQrCode(
                        credentials = localTestResult.credentials,
                        qrCodeWidth = binding.existingQr.cardQrImage.width,
                        qrCodeHeight = binding.existingQr.cardQrImage.height
                    )
                }
            }, {
                presentError()
            })
        }

        qrCodeViewModel.getLocalTestResult(OffsetDateTime.now())
    }
}