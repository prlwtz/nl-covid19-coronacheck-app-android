package nl.rijksoverheid.ctr.holder.ui.create_qr.digid

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import nl.rijksoverheid.ctr.holder.HolderStep.DigidNetworkRequest
import nl.rijksoverheid.ctr.holder.ui.create_qr.repositories.AuthenticationRepository
import nl.rijksoverheid.ctr.shared.livedata.Event
import nl.rijksoverheid.ctr.shared.models.OpenIdErrorResult.Error
import nl.rijksoverheid.ctr.shared.models.OpenIdErrorResult.ServerBusy

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class DigiDViewModel(private val authenticationRepository: AuthenticationRepository) : ViewModel() {

    private companion object {
        const val GENERIC_ERROR_TYPE = 0
        const val USER_CANCELLED_FLOW_CODE = 1
        const val LOGIN_REQUIRED_ERROR = "login_required"
        const val SAML_AUTHN_FAILED_ERROR = "saml_authn_failed"
    }

    val loading: LiveData<Event<Boolean>> = MutableLiveData()
    val digidResultLiveData = MutableLiveData<Event<DigidResult>>()

    fun login(
        activityResultLauncher: ActivityResultLauncher<Intent>,
        authService: AuthorizationService
    ) {
        (loading as MutableLiveData).value = Event(true)
        viewModelScope.launch {
            try {
                authenticationRepository.authResponse(activityResultLauncher, authService)
            } catch (e: Exception) {
                digidResultLiveData.postValue(
                    Event(DigidResult.Failed(Error(DigidNetworkRequest, e)))
                )
            }
            loading.value = Event(false)
        }
    }

    fun handleActivityResult(activityResult: ActivityResult, authService: AuthorizationService) {
        viewModelScope.launch {
            val intent = activityResult.data
            if (intent != null) {
                val authResponse = AuthorizationResponse.fromIntent(intent)
                val authError = AuthorizationException.fromIntent(intent)
                when {
                    authError != null -> postErrorResult(authError)
                    authResponse != null -> postResponseResult(authService, authResponse)
                    else -> postAuthNullResult()
                }
            } else {
                digidResultLiveData.postValue(Event(DigidResult.Cancelled))
            }
        }
    }

    private fun postErrorResult(authError: AuthorizationException) {
        val digidResult = when {
            isUserCancelled(authError) -> DigidResult.Cancelled
            isServerBusy(authError) -> DigidResult.Failed(ServerBusy(DigidNetworkRequest, authError))
            else -> DigidResult.Failed(Error(DigidNetworkRequest, authError))
        }
        digidResultLiveData.postValue(Event(digidResult))
    }

    private fun isServerBusy(authError: AuthorizationException) =
        authError.error == LOGIN_REQUIRED_ERROR || authError.error == SAML_AUTHN_FAILED_ERROR

    private fun isUserCancelled(authError: AuthorizationException) =
        authError.type == GENERIC_ERROR_TYPE && authError.code == USER_CANCELLED_FLOW_CODE

    private suspend fun postResponseResult(
        authService: AuthorizationService,
        authResponse: AuthorizationResponse
    ) {
        try {
            val jwt =
                authenticationRepository.jwt(authService, authResponse)
            digidResultLiveData.postValue(Event(DigidResult.Success(jwt)))
        } catch (e: Exception) {
            digidResultLiveData.postValue(
                Event(DigidResult.Failed(Error(DigidNetworkRequest, e)))
            )
        }
    }

    private fun postAuthNullResult() {
        digidResultLiveData.postValue(
            Event(DigidResult.Failed(Error(DigidNetworkRequest, NullPointerException())))
        )
    }
}
