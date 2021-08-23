package nl.rijksoverheid.ctr.api.factory

import nl.rijksoverheid.ctr.api.models.NetworkRequestResult
import nl.rijksoverheid.ctr.api.models.CoronaCheckErrorResponse
import nl.rijksoverheid.ctr.api.models.Step
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * This class should be used for every network request
 *
 */
class NetworkRequestResultFactory(
    private val errorResponseBodyConverter: Converter<ResponseBody, CoronaCheckErrorResponse>) {

    suspend fun <R: Any> createResult(
        step: Step,
        networkCall: suspend () -> R): NetworkRequestResult<R> {
        return try {
            val response = networkCall.invoke()
            NetworkRequestResult.Success(step, response)
        } catch (httpException: HttpException) {
            try {
                // Check if there is a error body
                val errorBody = httpException.response()?.errorBody() ?: return NetworkRequestResult.Failed.HttpError(step, httpException)

                // Check if the error body is a [CoronaCheckErrorResponse]
                val errorResponse = errorResponseBodyConverter.convert(errorBody) ?: return NetworkRequestResult.Failed.HttpError(step, httpException)

                return NetworkRequestResult.Failed.CoronaCheckHttpError(step, errorResponse)
            } catch (e: Exception) {
                return NetworkRequestResult.Failed.HttpError(step, httpException)
            }
        } catch (e: IOException) {
            when (e) {
                is SocketTimeoutException, is UnknownHostException -> {
                    NetworkRequestResult.Failed.NetworkError(step, e)
                }
                else -> NetworkRequestResult.Failed.Error(step, e)
            }
        } catch (e: Exception) {
            NetworkRequestResult.Failed.Error(step, e)
        }
    }
}