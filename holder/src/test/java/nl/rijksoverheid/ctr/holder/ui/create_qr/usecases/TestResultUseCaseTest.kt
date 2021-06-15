package nl.rijksoverheid.ctr.holder.ui.create_qr.usecases

import kotlinx.coroutines.runBlocking
import nl.rijksoverheid.ctr.holder.*
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.Holder
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.RemoteConfigProviders
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.RemoteTestResult2
import nl.rijksoverheid.ctr.holder.ui.create_qr.models.SignedResponseWithModel
import nl.rijksoverheid.ctr.holder.ui.create_qr.repositories.TestProviderRepository
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.time.OffsetDateTime

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
class TestResultUseCaseTest {

    @Test
    fun `testResult returns InvalidToken if a uniquecode has 1 part`() = runBlocking {
        val usecase = TestResultUseCase(
            configProviderUseCase = fakeConfigProviderUseCase(),
            testProviderRepository = fakeTestProviderRepository(),
            coronaCheckRepository = fakeCoronaCheckRepository(),
            commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
            secretKeyUseCase = fakeSecretKeyUseCase(),
            createCredentialUseCase = fakeCreateCredentialUseCase(),
            personalDetailsUtil = fakePersonalDetailsUtil(),
            cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
            testResultUtil = fakeTestResultUtil(),
            tokenValidatorUtil = fakeTokenValidatorUtil(),
            testResultAttributesUseCase = fakeTestResultAttributesUseCase()
        )
        val result = usecase.testResult(uniqueCode = "dummy")
        assertTrue(result is TestResult.InvalidToken)
    }

    @Test
    fun `testResult returns InvalidToken if a uniquecode has 2 parts`() = runBlocking {
        val usecase = TestResultUseCase(
            configProviderUseCase = fakeConfigProviderUseCase(),
            testProviderRepository = fakeTestProviderRepository(),
            coronaCheckRepository = fakeCoronaCheckRepository(),
            commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
            secretKeyUseCase = fakeSecretKeyUseCase(),
            createCredentialUseCase = fakeCreateCredentialUseCase(),
            personalDetailsUtil = fakePersonalDetailsUtil(),
            cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
            testResultUtil = fakeTestResultUtil(),
            tokenValidatorUtil = fakeTokenValidatorUtil(),
            testResultAttributesUseCase = fakeTestResultAttributesUseCase()
        )
        val result = usecase.testResult(uniqueCode = "dummy-dummy")
        assertTrue(result is TestResult.InvalidToken)
    }

    @Test
    fun `testResult returns InvalidToken if token validator fails`() = runBlocking {
        val usecase = TestResultUseCase(
            configProviderUseCase = fakeConfigProviderUseCase(),
            testProviderRepository = fakeTestProviderRepository(),
            coronaCheckRepository = fakeCoronaCheckRepository(),
            commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
            secretKeyUseCase = fakeSecretKeyUseCase(),
            createCredentialUseCase = fakeCreateCredentialUseCase(),
            personalDetailsUtil = fakePersonalDetailsUtil(),
            cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
            testResultUtil = fakeTestResultUtil(),
            tokenValidatorUtil = fakeTokenValidatorUtil(
                isValid = false
            ),
            testResultAttributesUseCase = fakeTestResultAttributesUseCase()
        )
        val result = usecase.testResult(uniqueCode = "provider-B-t1")
        assertTrue(result is TestResult.InvalidToken)
    }

    @Test
    fun `testResult returns InvalidToken if no provider matches`() = runBlocking {
        val usecase = TestResultUseCase(
            configProviderUseCase = fakeConfigProviderUseCase(),
            testProviderRepository = fakeTestProviderRepository(),
            coronaCheckRepository = fakeCoronaCheckRepository(),
            commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
            secretKeyUseCase = fakeSecretKeyUseCase(),
            createCredentialUseCase = fakeCreateCredentialUseCase(),
            personalDetailsUtil = fakePersonalDetailsUtil(),
            cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
            testResultUtil = fakeTestResultUtil(),
            tokenValidatorUtil = fakeTokenValidatorUtil(),
            testResultAttributesUseCase = fakeTestResultAttributesUseCase()
        )
        val result = usecase.testResult(uniqueCode = "provider-B-t1")
        assertTrue(result is TestResult.InvalidToken)
    }

    @Test
    fun `testResult returns NegativeTestResult if RemoteTestResult status is Complete and test result is valid`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    model =
                    getRemoteTestResult(status = RemoteTestResult2.Status.COMPLETE)
                ),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.NegativeTestResult)
        }

    @Test
    fun `testResult returns NoNegativeTestResult if RemoteTestResult status is Complete and test result is not valid`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    model =
                    getRemoteTestResult(status = RemoteTestResult2.Status.COMPLETE)
                ),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(isValid = false),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.NoNegativeTestResult)
        }

    @Test
    fun `testResult returns NoNegativeTestResult if RemoteTestResult status is Complete and test result is positive`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    model =
                    getRemoteTestResult(
                        status = RemoteTestResult2.Status.COMPLETE,
                        negativeResult = false
                    )
                ),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.NoNegativeTestResult)
        }

    @Test
    fun `testResult returns VerificationRequired if RemoteTestResult status is VerificationRequired`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    model =
                    getRemoteTestResult(status = RemoteTestResult2.Status.VERIFICATION_REQUIRED)
                ),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.VerificationRequired)
        }

    @Test
    fun `testResult returns InvalidToken if RemoteTestResult status is InvalidToken`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    model =
                    getRemoteTestResult(status = RemoteTestResult2.Status.INVALID_TOKEN)
                ),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.InvalidToken)
        }

    @Test
    fun `testResult returns ServerError if HttpException is thrown`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    remoteTestResultExceptionCallback = {
                        throw HttpException(
                            Response.error<String>(
                                400, "".toResponseBody()
                            )
                        )
                    }),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.ServerError)
        }

    @Test
    fun `testResult returns NetworkError if IOException is thrown`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = object : TestProviderRepository {
                    override suspend fun remoteTestResult(
                        url: String,
                        token: String,
                        verifierCode: String?,
                        signingCertificateBytes: ByteArray
                    ): SignedResponseWithModel<RemoteTestResult2> {
                        throw IOException()
                    }
                },
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.NetworkError)
        }

    @Test
    fun `testResult returns Pending if RemoteTestResult status is Pending`() =
        runBlocking {
            val providerIdentifier = "provider"
            val usecase = TestResultUseCase(
                configProviderUseCase = fakeConfigProviderUseCase(
                    provider = getRemoteTestProvider(
                        identifier = providerIdentifier
                    )
                ),
                testProviderRepository = fakeTestProviderRepository(
                    model =
                    getRemoteTestResult(status = RemoteTestResult2.Status.PENDING)
                ),
                coronaCheckRepository = fakeCoronaCheckRepository(),
                commitmentMessageUseCase = fakeCommitmentMessageUsecase(),
                secretKeyUseCase = fakeSecretKeyUseCase(),
                createCredentialUseCase = fakeCreateCredentialUseCase(),
                personalDetailsUtil = fakePersonalDetailsUtil(),
                cachedAppConfigUseCase = fakeCachedAppConfigUseCase(),
                testResultUtil = fakeTestResultUtil(),
                tokenValidatorUtil = fakeTokenValidatorUtil(),
                testResultAttributesUseCase = fakeTestResultAttributesUseCase()
            )
            val result = usecase.testResult(uniqueCode = "$providerIdentifier-B-t1")
            assertTrue(result is TestResult.Pending)
        }

    private fun getRemoteTestProvider(identifier: String): RemoteConfigProviders.TestProvider {
        return RemoteConfigProviders.TestProvider(
            name = "dummy",
            providerIdentifier = identifier,
            resultUrl = "dummy",
            publicKey = "dummy".toByteArray()
        )
    }

    private fun getRemoteTestResult(
        status: RemoteTestResult2.Status = RemoteTestResult2.Status.COMPLETE,
        negativeResult: Boolean = true
    ): SignedResponseWithModel<RemoteTestResult2> {
        return SignedResponseWithModel(
            rawResponse = "dummy".toByteArray(), model = RemoteTestResult2(
                result = RemoteTestResult2.Result(
                    unique = "dummy",
                    sampleDate = OffsetDateTime.now(),
                    testType = "dummy",
                    negativeResult = negativeResult,
                    holder = Holder(
                        firstNameInitial = "A",
                        lastNameInitial = "B",
                        birthDay = "1",
                        birthMonth = "2"
                    )
                ),
                providerIdentifier = "dummy",
                status = status,
                protocolVersion = "dummy"
            )
        )
    }
}
