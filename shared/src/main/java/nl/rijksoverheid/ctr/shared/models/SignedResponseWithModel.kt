/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *  Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *  SPDX-License-Identifier: EUPL-1.2
 *
 */

package nl.rijksoverheid.ctr.shared.models

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class SignedResponseWithModel<T>(val rawResponse: ByteArray, val model: T)