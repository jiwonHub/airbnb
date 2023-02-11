package com.example.airbnb

import retrofit2.Call
import retrofit2.http.GET

interface HouseService {
    @GET("/v3/bbd7f74e-2b42-4283-871b-4a4534fd2a2f") // mocky 라는 임의의 json 형식의 데이터를 가져오는 서비스 사용. 이 문자는 그 데이터의 주소값이다.
    fun getHouseList(): Call<HouseDto>
}