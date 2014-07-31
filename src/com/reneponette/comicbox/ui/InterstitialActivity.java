package com.reneponette.comicbox.ui;

import net.daum.adam.publisher.AdInterstitial;
import net.daum.adam.publisher.AdView.OnAdClosedListener;
import net.daum.adam.publisher.AdView.OnAdFailedListener;
import net.daum.adam.publisher.AdView.OnAdLoadedListener;
import net.daum.adam.publisher.impl.AdError;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class InterstitialActivity extends Activity {
	/** 전면형 광고 선언 */
	AdInterstitial mAdInterstitial = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 1. 전면형 광고 객체 생성
		mAdInterstitial = new AdInterstitial(this);
		// 2. 전면형 광고 클라이언트 ID를 설정한다.

		mAdInterstitial.setClientId("9785Z0pT1477ba9e003");
		// 3. (선택)전면형 광고 다운로드시에 실행할 리스너
		mAdInterstitial.setOnAdLoadedListener(new OnAdLoadedListener() {
			@Override
			public void OnAdLoaded() {
				Log.i("InterstitialTab", "광고가 로딩되었습니다.");
			}
		});
		// 4. (선택)전면형 광고 다운로드 실패시에 실행할 리스너
		mAdInterstitial.setOnAdFailedListener(new OnAdFailedListener() {
			@Override
			public void OnAdFailed(AdError error, String errorMessage) {
				Toast.makeText(InterstitialActivity.this, errorMessage, Toast.LENGTH_LONG).show();
			}
		});
		// 5. (선택)전면형 광고를 닫을 시에 실행할 리스너
		mAdInterstitial.setOnAdClosedListener(new OnAdClosedListener() {
			@Override
			public void OnAdClosed() {
				Log.i("InterstitialTab", "광고를 닫았습니다. ");
			}
		});
		// 6. 전면형 광고를 불러온다.
		mAdInterstitial.loadAd();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mAdInterstitial != null) {
			mAdInterstitial = null;
		}
	}
}