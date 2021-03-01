package nl.rijksoverheid.ctr.holder.myoverview.date_of_birth

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import nl.rijksoverheid.ctr.shared.livedata.Event

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class DateOfBirthInputViewModel : ViewModel() {
    val dateOfBirthMillisLiveData = MutableLiveData<Event<Long>>()
    val retrievedDateOfBirth: Long?
        get() = dateOfBirthMillisLiveData.value?.peekContent()

    fun setDateOfBirthMillis(millis: Long) {
        dateOfBirthMillisLiveData.postValue(Event(millis))
    }
}
