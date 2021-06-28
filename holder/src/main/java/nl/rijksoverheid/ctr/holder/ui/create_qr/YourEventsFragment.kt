/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.holder.ui.create_qr

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import nl.rijksoverheid.ctr.appconfig.usecases.CachedAppConfigUseCase
import nl.rijksoverheid.ctr.design.ext.formatDateTime
import nl.rijksoverheid.ctr.design.ext.formatDayMonthYear
import nl.rijksoverheid.ctr.design.ext.formatMonth
import nl.rijksoverheid.ctr.design.utils.DialogUtil
import nl.rijksoverheid.ctr.holder.HolderMainFragment
import nl.rijksoverheid.ctr.holder.R
import nl.rijksoverheid.ctr.holder.databinding.FragmentYourEventsBinding
import nl.rijksoverheid.ctr.holder.persistence.database.DatabaseSyncerResult
import nl.rijksoverheid.ctr.holder.persistence.database.entities.EventType
import nl.rijksoverheid.ctr.holder.persistence.database.entities.OriginType
import nl.rijksoverheid.ctr.holder.ui.create_qr.items.YourEventWidget
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.util.InfoScreenUtil
import nl.rijksoverheid.ctr.shared.ext.findNavControllerSafety
import nl.rijksoverheid.ctr.shared.livedata.EventObserver
import nl.rijksoverheid.ctr.shared.utils.PersonalDetailsUtil
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class YourEventsFragment : Fragment(R.layout.fragment_your_events) {

    private val args: YourEventsFragmentArgs by navArgs()

    private val cachedAppConfigUseCase: CachedAppConfigUseCase by inject()
    private val personalDetailsUtil: PersonalDetailsUtil by inject()
    private val infoScreenUtil: InfoScreenUtil by inject()
    private val dialogUtil: DialogUtil by inject()

    private val yourEventsViewModel: YourEventsViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentYourEventsBinding.bind(view)

        presentHeader(
            binding = binding
        )

        presentEvents(
            binding = binding
        )

        presentFooter(
            binding = binding
        )

        handleButton(
            binding = binding
        )

        handleBackButton()

        yourEventsViewModel.loading.observe(viewLifecycleOwner, EventObserver {
            (parentFragment?.parentFragment as HolderMainFragment).presentLoading(it)
            binding.bottom.setButtonEnabled(!it)
        })

        yourEventsViewModel.yourEventsResult.observe(
            viewLifecycleOwner,
            EventObserver { databaseSyncerResult ->
                when (databaseSyncerResult) {
                    is DatabaseSyncerResult.Success -> {
                        // We have a origin in the database that we expect, so success
                        findNavController().navigate(
                            YourEventsFragmentDirections.actionMyOverview()
                        )
                    }
                    is DatabaseSyncerResult.MissingOrigin -> {
                        findNavController().navigate(
                            YourEventsFragmentDirections.actionCouldNotCreateQr(
                                toolbarTitle = args.toolbarTitle,
                                title = getString(R.string.rule_engine_no_origin_title),
                                description = getString(R.string.rule_engine_no_test_origin_description)
                            )
                        )
                    }
                    is DatabaseSyncerResult.NetworkError -> {
                        dialogUtil.presentDialog(
                            context = requireContext(),
                            title = R.string.dialog_no_internet_connection_title,
                            message = getString(R.string.dialog_no_internet_connection_description),
                            positiveButtonText = R.string.dialog_close,
                            positiveButtonCallback = {}
                        )
                    }
                    is DatabaseSyncerResult.ServerError -> {
                        dialogUtil.presentDialog(
                            context = requireContext(),
                            title = R.string.dialog_error_title,
                            message = getString(
                                R.string.dialog_error_message_with_error_code,
                                databaseSyncerResult.httpCode.toString()
                            ),
                            positiveButtonText = R.string.dialog_close,
                            positiveButtonCallback = {}
                        )
                    }
                }
            })
    }

    private fun presentHeader(binding: FragmentYourEventsBinding) {
        when (args.type) {
            is YourEventsFragmentType.TestResult2,
            is YourEventsFragmentType.RemoteProtocol3Type.NegativeTests -> {
                binding.title.setText(R.string.your_negative_test_results_title)
                binding.description.setHtmlText(getString(R.string.your_negative_test_results_description))
            }
            is YourEventsFragmentType.RemoteProtocol3Type.Vaccinations -> {
                binding.title.visibility = View.GONE
                binding.description.text =
                    getString(R.string.your_retrieved_vaccinations_description)
            }
            is YourEventsFragmentType.RemoteProtocol3Type.PositiveTestsAndRecoveries -> {
                binding.title.visibility = View.GONE
                binding.description.text = getString(R.string.your_positive_test_description)
            }
        }
    }

    private fun presentEvents(binding: FragmentYourEventsBinding) {
        when (val type = args.type) {
            is YourEventsFragmentType.TestResult2 -> {
                presentTestResult2(
                    binding = binding,
                    remoteProtocol2 = type.remoteTestResult
                )
            }
            is YourEventsFragmentType.RemoteProtocol3Type -> {
                val remoteEvents = type.remoteEvents.map { it.key }

                if (hasShownExpiredEvent(remoteEvents)) return

                remoteEvents.forEach { remoteProtocol3 ->
                    remoteProtocol3.events?.forEach { remoteEvent ->
                        when (remoteEvent) {
                            is RemoteEventVaccination -> {
                                presentVaccinationEvent(
                                    binding = binding,
                                    providerIdentifier = remoteProtocol3.providerIdentifier,
                                    fullName = getFullName(remoteProtocol3.holder),
                                    birthDate = getBirthDate(remoteProtocol3.holder),
                                    event = remoteEvent
                                )
                            }
                            is RemoteEventNegativeTest -> {
                                presentNegativeTestEvent(
                                    binding = binding,
                                    fullName = getFullName(remoteProtocol3.holder),
                                    birthDate = getBirthDate(remoteProtocol3.holder),
                                    event = remoteEvent
                                )
                            }
                            is RemoteEventPositiveTest -> {
                                presentPositiveTestEvent(
                                    binding = binding,
                                    fullName = getFullName(remoteProtocol3.holder),
                                    birthDate = getBirthDate(remoteProtocol3.holder),
                                    event = remoteEvent
                                )
                            }
                            is RemoteEventRecovery -> {
                                presentRecoveryEvent(
                                    binding = binding,
                                    fullName = getFullName(remoteProtocol3.holder),
                                    birthDate = getBirthDate(remoteProtocol3.holder),
                                    event = remoteEvent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun hasShownExpiredEvent(remoteEvents: List<RemoteProtocol3>): Boolean {
        return remoteEvents.flatMap { it.events ?: emptyList() }
            .filter { it is RemoteEventPositiveTest || it is RemoteEventRecovery }
            .takeIf { it.size == 1 }
            ?.map {
                val date = (it as? RemoteEventPositiveTest)?.getDate()
                    ?: (it as? RemoteEventRecovery)?.getDate()

                if (OffsetDateTime.now().minusDays(180) > date) {
                    findNavControllerSafety(R.id.nav_your_events)?.navigate(
                        YourEventsFragmentDirections.actionExpiredTestResult()
                    )
                    true
                } else false
            }?.firstOrNull() ?: false
    }

    private fun presentTestResult2(
        binding: FragmentYourEventsBinding,
        remoteProtocol2: RemoteTestResult2
    ) {
        remoteProtocol2.result?.let { result ->
            val personalDetails = personalDetailsUtil.getPersonalDetails(
                firstNameInitial = result.holder.firstNameInitial,
                lastNameInitial = result.holder.lastNameInitial,
                birthDay = result.holder.birthDay,
                birthMonth = result.holder.birthMonth,
                includeBirthMonthNumber = false
            )

            val testDate = OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(result.sampleDate.toEpochSecond()),
                ZoneOffset.UTC
            ).formatDateTime(requireContext())

            val infoScreen = infoScreenUtil.getForRemoteTestResult2(
                result = remoteProtocol2.result,
                personalDetails = personalDetails,
                testDate = testDate
            )

            val eventWidget = YourEventWidget(requireContext()).apply {
                setContent(
                    title = getString(R.string.your_negative_test_results_row_title),
                    subtitle = getString(
                        R.string.your_negative_test_results_row_subtitle,
                        testDate,
                        result.sampleDate.plusHours(
                            cachedAppConfigUseCase.getCachedAppConfigMaxValidityHours().toLong()
                        ).formatDateTime(requireContext()),
                        "${personalDetails.firstNameInitial} ${personalDetails.lastNameInitial} ${personalDetails.birthDay} ${personalDetails.birthMonth}"
                    ),
                    infoClickListener = {
                        findNavController().navigate(
                            YourEventsFragmentDirections.actionShowExplanation(
                                toolbarTitle = infoScreen.title,
                                description = infoScreen.description
                            )
                        )
                    }
                )
            }
            binding.eventsGroup.addView(eventWidget)
        }
    }

    private fun presentVaccinationEvent(
        binding: FragmentYourEventsBinding,
        providerIdentifier: String,
        fullName: String,
        birthDate: String,
        event: RemoteEventVaccination
    ) {
        val infoScreen = infoScreenUtil.getForVaccination(
            event = event,
            fullName = fullName,
            birthDate = birthDate
        )

        val eventWidget = YourEventWidget(requireContext()).apply {
            setContent(
                title = resources.getString(
                    R.string.retrieved_vaccination_title,
                    event.vaccination?.date?.formatMonth(),
                    cachedAppConfigUseCase.getProviderName(providerIdentifier)
                ),
                subtitle = resources.getString(
                    R.string.your_vaccination_row_subtitle,
                    fullName,
                    birthDate
                ),
                infoClickListener = {
                    findNavController().navigate(
                        YourEventsFragmentDirections.actionShowExplanation(
                            toolbarTitle = infoScreen.title,
                            description = infoScreen.description
                        )
                    )
                }
            )
        }
        binding.eventsGroup.addView(eventWidget)
    }

    private fun presentNegativeTestEvent(
        binding: FragmentYourEventsBinding,
        fullName: String,
        birthDate: String,
        event: RemoteEventNegativeTest
    ) {
        val testDate = event.negativeTest?.sampleDate?.formatDateTime(requireContext()) ?: ""

        val infoScreen = infoScreenUtil.getForNegativeTest(
            event = event,
            fullName = fullName,
            testDate = testDate,
            birthDate = birthDate
        )

        val eventWidget = YourEventWidget(requireContext()).apply {
            setContent(
                title = getString(R.string.your_negative_test_results_row_title),
                subtitle = getString(
                    R.string.your_negative_test_3_0_results_row_subtitle,
                    testDate,
                    fullName,
                    birthDate
                ),
                infoClickListener = {
                    findNavController().navigate(
                        YourEventsFragmentDirections.actionShowExplanation(
                            toolbarTitle = infoScreen.title,
                            description = infoScreen.description
                        )
                    )
                }
            )
        }
        binding.eventsGroup.addView(eventWidget)
    }

    private fun presentPositiveTestEvent(
        binding: FragmentYourEventsBinding,
        fullName: String,
        birthDate: String,
        event: RemoteEventPositiveTest
    ) {
        val testDate = event.positiveTest?.sampleDate?.formatDateTime(requireContext()) ?: ""

        val infoScreen = infoScreenUtil.getForPositiveTest(
            event = event,
            testDate = testDate,
            fullName = fullName,
            birthDate = birthDate
        )

        val eventWidget = YourEventWidget(requireContext()).apply {
            setContent(
                title = getString(R.string.positive_test_title),
                subtitle = getString(
                    R.string.your_negative_test_3_0_results_row_subtitle,
                    testDate,
                    fullName,
                    birthDate
                ),
                infoClickListener = {
                    findNavController().navigate(
                        YourEventsFragmentDirections.actionShowExplanation(
                            toolbarTitle = infoScreen.title,
                            description = infoScreen.description
                        )
                    )
                }
            )
        }
        binding.eventsGroup.addView(eventWidget)
    }

    private fun presentRecoveryEvent(
        binding: FragmentYourEventsBinding,
        fullName: String,
        birthDate: String,
        event: RemoteEventRecovery
    ) {
        val testDate = event.recovery?.sampleDate?.formatDayMonthYear() ?: ""

        val infoScreen = infoScreenUtil.getForRecovery(
            event = event,
            fullName = fullName,
            testDate = testDate,
            birthDate = birthDate
        )

        val eventWidget = YourEventWidget(requireContext()).apply {
            setContent(
                title = getString(R.string.positive_test_title),
                subtitle = getString(
                    R.string.your_negative_test_3_0_results_row_subtitle,
                    testDate,
                    fullName,
                    birthDate
                ),
                infoClickListener = {
                    findNavController().navigate(
                        YourEventsFragmentDirections.actionShowExplanation(
                            toolbarTitle = infoScreen.title,
                            description = infoScreen.description
                        )
                    )
                }
            )
        }
        binding.eventsGroup.addView(eventWidget)
    }

    private fun handleButton(binding: FragmentYourEventsBinding) {
        binding.bottom.setButtonClick {
            when (val type = args.type) {
                is YourEventsFragmentType.TestResult2 -> {
                    yourEventsViewModel.saveNegativeTest2(
                        remoteTestResult = type.remoteTestResult,
                        rawResponse = type.rawResponse
                    )
                }
                is YourEventsFragmentType.RemoteProtocol3Type -> {
                    val eventType = when (type) {
                        is YourEventsFragmentType.RemoteProtocol3Type.Vaccinations -> EventType.Vaccination
                        is YourEventsFragmentType.RemoteProtocol3Type.NegativeTests -> EventType.Test
                        is YourEventsFragmentType.RemoteProtocol3Type.PositiveTestsAndRecoveries -> EventType.Recovery
                    }
                    val originType = when (type) {
                        is YourEventsFragmentType.RemoteProtocol3Type.Vaccinations -> OriginType.TYPE_VACCINATION
                        is YourEventsFragmentType.RemoteProtocol3Type.NegativeTests -> OriginType.TYPE_TEST
                        is YourEventsFragmentType.RemoteProtocol3Type.PositiveTestsAndRecoveries -> OriginType.TYPE_RECOVERY
                    }
                    yourEventsViewModel.saveRemoteProtocol3Events(
                        remoteProtocols3 = type.remoteEvents,
                        originType = originType,
                        eventType = eventType
                    )
                }
            }
        }
    }

    private fun presentFooter(binding: FragmentYourEventsBinding) {
        binding.somethingWrongButton.setOnClickListener {
            findNavController().navigate(
                YourEventsFragmentDirections.actionShowSomethingWrong(
                    description = if (args.type is YourEventsFragmentType.RemoteProtocol3Type.Vaccinations) {
                        getString(R.string.dialog_vaccination_something_wrong_description)
                    } else {
                        getString(R.string.dialog_negative_test_result_something_wrong_description)
                    }
                )
            )
        }
    }

    private fun handleBackButton() {
        if (args.type is YourEventsFragmentType.RemoteProtocol3Type.Vaccinations) {
            blockBackButton(
                title = R.string.retrieved_vaccinations_backbutton_title,
                message = R.string.retrieved_vaccinations_backbutton_message
            )
        } else {
            blockBackButton(
                title = R.string.your_negative_test_results_backbutton_title,
                message = R.string.your_negative_test_results_backbutton_message
            )
        }
    }

    private fun blockBackButton(@StringRes title: Int, @StringRes message: Int) {
        // Catch back button to show modal instead
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(title))
                    .setMessage(getString(message))
                    .setPositiveButton(R.string.your_negative_test_results_backbutton_ok) { _, _ ->
                        findNavController().navigate(
                            YourEventsFragmentDirections.actionMyOverview()
                        )
                    }
                    .setNegativeButton(R.string.your_negative_test_results_backbutton_cancel) { _, _ -> }
                    .show()
            }
        })
    }

    private fun getFullName(holder: RemoteProtocol3.Holder?): String = holder?.let {
        return if (it.infix.isNullOrEmpty()) {
            "${it.lastName}, ${it.firstName}"
        } else {
            "${it.infix} ${it.lastName}, ${it.firstName}"
        }
    } ?: ""

    private fun getBirthDate(holder: RemoteProtocol3.Holder?): String =
        holder?.birthDate?.let { birthDate ->
            try {
                LocalDate.parse(birthDate, DateTimeFormatter.ISO_DATE).formatDayMonthYear()
            } catch (e: Exception) {
                ""
            }
        } ?: ""
}

