package com.example.bigpengweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bigpengweather.db.City;
import com.example.bigpengweather.db.County;
import com.example.bigpengweather.db.Province;
import com.example.bigpengweather.util.HttpUtil;
import com.example.bigpengweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by 易然 on 2017/8/21.
 */

public class Choose_AreeaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY= 0;
    public static final int LEVEL_COUNTY = 0;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String > adapter;
    private List<String> datalist = new ArrayList<>();
    /**
     *省列表
     */
    private  List<Province> provinceList;
    /**
     * 市列表
     */
    private  List<City> cityList;
    /**
     * 县/区列表
     */
    private  List<County> countyList;
    /**
     * 选中的省份
     */
    private  Province selectedProvince;
    /**
     * 选中的城市
     */
    private  City selectedCity;
    /**
     *  当前选中的级别
     */
    private int currenLevel;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,datalist);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currenLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if(currenLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currenLevel == LEVEL_COUNTY){
                    queryCities();

                }else if (currenLevel == LEVEL_CITY){
                    queryProvinces();

                }
            }
        });
        queryProvinces();
    }
    /**
     *  查询全国所有的省优先从数据库中查询如果没有查询到再去服务器上查询
     */
    private  void queryProvinces(){
        titleText.setText("中国 ");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0){
            datalist.clear();
            for (Province province : provinceList){
                datalist.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenLevel = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServer(address,"province");
        }
    }
    /**
     * 查询选中省内所有的市优先从数据库查询如果没有查询到再去服务器上查询
     */
    private void queryCities(){
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0){
            datalist.clear();
            for (City city : cityList){
                datalist.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenLevel = LEVEL_CITY;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address,"city");
        }

    }
    /**
     * 查询选中省内所有的县优先从数据库查询如果没有查询到再去服务器上查询
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0){
            datalist.clear();
            for (County county : countyList){
                datalist.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenLevel = LEVEL_COUNTY;
        }else {
            int cityCode = selectedCity.getCityCode();
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode +"/" +cityCode;
            queryFromServer(address,"county");
        }

    }



    /**
     * 根据传入的地址和类型从服务器上查询省市县的数据
     */
    private void queryFromServer(String address, final  String type) {
        showProgresDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals((type))){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }

                    });
                }
            }
            @Override
            public void onFailure(Call call,IOException e){
                //通过runonuithread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @RequiresApi(api = Build.VERSION_CODES.M)
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


    }

    /**
     *  显示精度对话框
     */
    private void showProgresDialog() {
    if (progressDialog == null){
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("玩命加载中...");
        progressDialog.setCanceledOnTouchOutside(false);
    }
    progressDialog.show();
    }
    /**
     *  关闭对话框
     */

    private void closeProgressDialog() {
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
