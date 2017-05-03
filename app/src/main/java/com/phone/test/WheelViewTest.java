package com.phone.test;

import android.app.Activity;
import android.os.Bundle;

import com.phone.wheelview.WheelView;

import java.util.ArrayList;
import java.util.List;

import custom.phone.com.mycustomview.R;

/**
 * Created by Phone on 2017/4/22.
 */

public class WheelViewTest extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_wheel);
		WheelView wheelView = (WheelView) findViewById(R.id.wheelView);
		wheelView.setDatas(generateDatas());
	}

	private List<String> generateDatas() {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < 30; i++) {
			list.add("item" + i);
		}
		return list;
	}
}
