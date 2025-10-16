package com.niki914.chat.inner

@Deprecated(level = DeprecationLevel.ERROR, message = "不用 retrofit 了")
class IChatService
//internal interface IChatService {
//    @Keep
//    @POST("v1/chat/completions")
//    suspend fun chat(
//        @HeaderMap headers: Map<String, String>,
//        @Body requestBody: ChatCompletionRequest
//    ): Response<ResponseBody>

//        @POST("chat/completions")
//        @Keep
//        suspend fun chat2(
//            @HeaderMap headers: Map<String, String>,
//            @Body requestBody: ChatCompletionRequest
//        ): Call<ResponseBody>
//}