package com.example.airbnb

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.example.airbnb.databinding.ActivityMainBinding
import com.example.airbnb.databinding.BottomSheetBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.util.FusedLocationSource
import com.naver.maps.map.util.MarkerIcons
import com.naver.maps.map.widget.LocationButtonView
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
class MainActivity : AppCompatActivity(), OnMapReadyCallback, Overlay.OnClickListener {

    private lateinit var naverMap: NaverMap
    private lateinit var locationSource: FusedLocationSource
    private lateinit var binding: ActivityMainBinding
    private val mapView: MapView by lazy {
        binding.mapView
    }
    private val viewPager: ViewPager2 by lazy {
        binding.houseViewPager
    }
    private val recyclerView: RecyclerView by lazy {
        findViewById(R.id.recyclerView)
    }
    private val currentLocationButton: LocationButtonView by lazy {
        findViewById(R.id.currentLocationButton)
    }
    private val viewPagerAdapter = HouseViewPagerAdapter()
    private val recyclerAdapter = HouseListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        viewPager.adapter = viewPagerAdapter
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                val selectedHouseModel = viewPagerAdapter.currentList[position]
                val cameraUpdate = CameraUpdate.scrollTo(LatLng(selectedHouseModel.lat, selectedHouseModel.lng))
                    .animate(CameraAnimation.Easing)

                naverMap.moveCamera(cameraUpdate)
            }
        })
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        naverMap.maxZoom = 18.0 // 최대 줌
        naverMap.minZoom = 10.0 // 최소 줌

        val cameraUpdate = CameraUpdate.scrollTo(LatLng(37.497898550942466, 127.02768639039702)) // 초기 화면 설정
        naverMap.moveCamera(cameraUpdate)

        val uiSetting = naverMap.uiSettings // 현위치 버튼
        uiSetting.isLocationButtonEnabled = false // 현위치 버튼 활성화..
        currentLocationButton.map = naverMap // 현위치 버튼 위치 재설정

        // 현위치 사용 권한을 받고 onRequestPermissionsResult()로 승인이 되면 현위치 기능 사용 가능
        locationSource = FusedLocationSource(this@MainActivity, LOCATION_PERMISSION_REQUEST_CODE)
        naverMap.locationSource = locationSource // locationSource 를 네이버맵에서도 받아서 콜백을 받기위해서

        getHouseListFromAPI()
    }

    private fun getHouseListFromAPI(){
        val retrofit = Retrofit.Builder()
            .baseUrl("https://run.mocky.io") // mocky 에 저장된 json 데이터를 가져오기위한 mocky url
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofit.create(HouseService::class.java).also {
            it.getHouseList()
                .enqueue(object : Callback<HouseDto>{
                    override fun onResponse(call: Call<HouseDto>, response: Response<HouseDto>) { // 성공 처리
                        if(response.isSuccessful.not()){
                            return
                        }

                        response.body()?.let { dto ->
                            updateMarker(dto.items)
                            viewPagerAdapter.submitList(dto.items)
                            recyclerAdapter.submitList(dto.items)
                        }
                    }

                    override fun onFailure(call: Call<HouseDto>, t: Throwable) { // 실패 처리

                    }

                })
        }
    }

    private fun updateMarker(houses: List<HouseModel>){ // 마커 찍기
        houses.forEach{ house -> 
            val marker = Marker()
            marker.position = LatLng(house.lat, house.lng)
            marker.onClickListener = this
            marker.map = naverMap
            marker.tag = house.id
            marker.icon = MarkerIcons.BLACK
            marker.iconTintColor = Color.RED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE){
            return
        }

        if (locationSource.onRequestPermissionsResult(requestCode, permissions, grantResults)){
            if (!locationSource.isActivated){ // locationSource 가 액티브하지않으면
                naverMap.locationTrackingMode = LocationTrackingMode.None // 권한 거부
            }
            return
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    companion object{
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }

    override fun onClick(overlay: Overlay): Boolean {
        overlay.tag

        val selectedModel = viewPagerAdapter.currentList.firstOrNull(){
            it.id == overlay.tag
        }

        selectedModel?.let{
            val position = viewPagerAdapter.currentList.indexOf(it)
            viewPager.currentItem = position
        }
        return true
    }
}