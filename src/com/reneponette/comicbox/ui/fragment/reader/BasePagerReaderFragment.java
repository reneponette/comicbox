package com.reneponette.comicbox.ui.fragment.reader;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

import net.daum.adam.publisher.AdInterstitial;
import net.daum.adam.publisher.AdView.OnAdFailedListener;
import net.daum.adam.publisher.AdView.OnAdLoadedListener;
import net.daum.adam.publisher.impl.AdError;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.constant.C;
import com.reneponette.comicbox.controller.PageBuilder;
import com.reneponette.comicbox.db.FileInfo;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageType;
import com.reneponette.comicbox.ui.FileSettingsActivity;
import com.reneponette.comicbox.ui.ReaderActivity;
import com.reneponette.comicbox.utils.DialogHelper;
import com.reneponette.comicbox.utils.Logger;
import com.reneponette.comicbox.utils.MetricUtils;
import com.reneponette.comicbox.view.ExtendedViewPager;
import com.reneponette.comicbox.view.TouchImageView;
import com.reneponette.comicbox.view.TouchImageView.OnSideTouchListener;

@SuppressWarnings("deprecation")
public class BasePagerReaderFragment extends BaseReaderFragment {

	protected ExtendedViewPager viewPager;
	protected PagerAdapter pagerAdapter;

	protected View menuContainer;
	protected TextView filename;
	protected SeekBar seekBar;

	View previewBox;
	TextView previewPageNumTv;
	ImageView previewIv;

	Timer previewLoadTimer;

	private boolean use3Fingers;

	AdInterstitial mAdInterstitial;

	int curPageIndex;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Logger.i(this, "onSaveInstanceState() - " + this);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Logger.i(this, "onCreate(), savedInstanceState = " + savedInstanceState + " - " + this);

		pagerAdapter = new TouchImageAdapter(pageBuilder);

		setupAdam();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Logger.i(this, "onCreateView() - " + this);

		View rootView = inflater.inflate(R.layout.fragment_reader_zip, container, false);

		viewPager = (ExtendedViewPager) rootView.findViewById(R.id.view_pager);
		viewPager.setAdapter(pagerAdapter);

		// 프리뷰 설정
		menuContainer = rootView.findViewById(R.id.menu_container);
		menuContainer.setVisibility(View.GONE);

		filename = (TextView) rootView.findViewById(R.id.filename);

		rootView.findViewById(R.id.setting_btn).setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				showSettingsActivity();
			}
		});

		previewBox = rootView.findViewById(R.id.preview_box);
		previewPageNumTv = (TextView) rootView.findViewById(R.id.preview_page_num);
		previewIv = (ImageView) rootView.findViewById(R.id.preview_image);

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar1);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				previewBox.setVisibility(View.GONE);
				int position = seekBar.getProgress();
				viewPager.setCurrentItem(position);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				previewBox.setVisibility(View.VISIBLE);
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
				if (fromUser) {
					PageBuilder controller = pageBuilder;
					if (controller.getReadDirection() == ReadDirection.RTL) {
						previewPageNumTv.setText((controller.pageSize() - progress) + "");
					} else {
						previewPageNumTv.setText(progress + 1 + "");
					}

					if (previewLoadTimer != null)
						previewLoadTimer.cancel();
					previewLoadTimer = new Timer();
					previewLoadTimer.schedule(new TimerTask() {

						@Override
						public void run() {
							Logger.i(this, "previewLoadTimer Fired!");
							final Bitmap previewBitmap = getPreviewBitmap(previewIv, progress);
							getActivity().runOnUiThread(new Runnable() {

								@Override
								public void run() {
									if (previewBitmap != null)
										previewIv.setImageBitmap(previewBitmap);
								}
							});
							previewLoadTimer = null;
						}
					}, 500);
				}
			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Logger.i(this, "onViewCreated() - " + this);
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				curPageIndex = position;

				seekBar.setProgress(position);

				PageInfo pi = pageBuilder.getPageInfo(position);
				filename.setText(pi.getName());

				if (checkEndPage(position) && C.ENABLE_ADAM)
					mAdInterstitial.loadAd();
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// TODO Auto-generated method stub

			}
		});

		pageBuilder.build();
	}

	@Override
	public void onResume() {
		super.onResume();
		Logger.i(this, "onResume() - " + this);
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		use3Fingers = settings.getBoolean("viewer_use_3_fingers", false);
	}

	@Override
	public void onDestroy() {
		Logger.e(this, "onDestroy() - " + this);

		pageBuilder.setOnDataBuildListener(null);
		pageBuilder.stop();

		int pageCount = pageBuilder.getPageInfoList().size();
		int curPageNum = pageBuilder.getReadDirection() == ReadDirection.RTL ? pageCount - curPageIndex
				: curPageIndex + 1;
		if (curPageNum == pageCount) {
			pageBuilder.saveReadState(-1);
		} else {
			pageBuilder.saveReadState(viewPager.getCurrentItem());
		}

		if (mAdInterstitial != null) {
			mAdInterstitial = null;
		}
		super.onDestroy();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != Activity.RESULT_OK) {
			super.onActivityResult(requestCode, resultCode, data);
			return;
		}
		if (requestCode == REQ_SETTINGS) {
			getActivity().finish();

			FileInfo info = pageBuilder.getFileInfo();
			Intent intent = ReaderActivity.newIntent(getActivity(), info);
			startActivity(intent);
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/*---------------------------------------------------------------------------*/

	@Override
	public boolean onBackPressed() {
		if (menuContainer.getVisibility() == View.VISIBLE) {
			menuContainer.setVisibility(View.GONE);
			return true;
		}
		return false;
	}

	private boolean checkEndPage(int position) {
		boolean isEndPage = false;
		if (pageBuilder.getReadDirection() == ReadDirection.RTL) {
			isEndPage = position == 0;
		} else {
			isEndPage = position == pageBuilder.pageSize() - 1;
		}

		if (isEndPage) {
			onGoNextFile();
			return true;
		}
		return false;
	}

	protected void onGoNextFile() {
		pageBuilder.saveReadState(-1);

		// 다음 권으로 넘김
		DialogHelper.showGoNextComicsDialog(getActivity(), new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {

				File next = findNextFile();
				if (next != null) {
					menuContainer.setVisibility(View.GONE);
					pageBuilder.prepare(next);
					pageBuilder.build();
				}
			}
		});
	}

	protected Bitmap getPreviewBitmap(ImageView iv, int position) {
		return null;
	}

	protected Bitmap getPageBitmap(ImageView iv, int position) {
		return null;
	}

	@Override
	protected void onMoveToLeftPage() {
		int curIndex = viewPager.getCurrentItem();
		curIndex--;
		if (curIndex < 0) {
			curIndex = 0;
		}
		viewPager.setCurrentItem(curIndex, false);
	}

	@Override
	protected void onMoveToRightPage() {
		int curIndex = viewPager.getCurrentItem();
		curIndex++;
		if (curIndex >= pagerAdapter.getCount()) {
			curIndex = pagerAdapter.getCount() - 1;
		}
		viewPager.setCurrentItem(curIndex, false);
	}

	private void showSettingsActivity() {
		// 설정 액티비티 띠우기
		pageBuilder.saveReadState(viewPager.getCurrentItem());
		startActivityForResult(FileSettingsActivity.newIntent(getActivity(), pageBuilder.getFileInfo()), REQ_SETTINGS);
	}

	protected void initUI() {
		FileMeta meta = pageBuilder.getFileInfo().getMeta();
		ReadDirection readDirection = pageBuilder.getReadDirection();
		int pagesPerScanComputed = pageBuilder.getPagesPerScan();
		int pageSize = pageBuilder.pageSize();

		int startPageIndex = meta.lastReadPageIndex;
		if (startPageIndex == -1) // 처음 보는 경우
			startPageIndex = readDirection == ReadDirection.RTL ? pageSize - 1 : 0;
		else {
			if (readDirection != meta.lastReadDirection) {
				if (pagesPerScanComputed != meta.lastPagesPerScan) {
					if (meta.lastPagesPerScan == 2) {
						// 2 -> 1
						startPageIndex = pageSize - 1 - meta.lastReadPageIndex / 2;
					} else {
						// 1 -> 2
						startPageIndex = pageSize - 1 - meta.lastReadPageIndex * 2;
					}
				} else
					startPageIndex = pageSize - 1 - meta.lastReadPageIndex;
			} else {
				if (pagesPerScanComputed != meta.lastPagesPerScan) {
					if (meta.lastPagesPerScan == 2) {
						// 2 -> 1
						startPageIndex = meta.lastReadPageIndex / 2;
					} else {
						// 1 -> 2
						startPageIndex = meta.lastReadPageIndex * 2;
					}
				}

			}
		}

		viewPager.setCurrentItem(startPageIndex, false);
		seekBar.setMax(pageSize - 1);
		seekBar.setProgress(startPageIndex);

		updateSeekBarLabel();
	}

	protected void updateSeekBarLabel() {
		if (pageBuilder.getReadDirection() == ReadDirection.RTL) {
			((TextView) getView().findViewById(R.id.pageRight)).setText("1");
			((TextView) getView().findViewById(R.id.pageLeft)).setText(pageBuilder.pageSize() + "");
		} else {
			((TextView) getView().findViewById(R.id.pageLeft)).setText("1");
			((TextView) getView().findViewById(R.id.pageRight)).setText(pageBuilder.pageSize() + "");
		}

	}

	private void setupAdam() {

		if (C.ENABLE_ADAM) {
			// 1. 전면형 광고 객체 생성
			mAdInterstitial = new AdInterstitial(getActivity());
			// mAdInterstitial.setClientId("InterstitialTestClientId");
			// mAdInterstitial.setAdInfo(new AdInfo());
			mAdInterstitial.setClientId("9c4fZ0GT1488247588d");
			mAdInterstitial.setOnAdLoadedListener(new OnAdLoadedListener() {
				@Override
				public void OnAdLoaded() {
					Log.i("InterstitialTab", "광고가 로딩되었습니다.");
				}
			});
			mAdInterstitial.setOnAdFailedListener(new OnAdFailedListener() {
				@Override
				public void OnAdFailed(AdError error, String errorMessage) {
					Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	/*------------Adapters-----------------------------------------------*/

	private class TouchImageAdapter extends PagerAdapter {

		PageBuilder controller;

		public TouchImageAdapter(PageBuilder controller) {
			this.controller = controller;
		}

		@Override
		public int getCount() {
			return controller.pageSize();
		}

		@Override
		public View instantiateItem(ViewGroup container, int position) {
			TouchImageView iv = new TouchImageView(container.getContext());

			setupImageView(iv);

			PageInfo info = controller.getPageInfo(position);

			if (info.getType() == PageType.END) {
				View v = getActivity().getLayoutInflater().inflate(R.layout.page_end_item, null);
				container.addView(v);
				return v;
			}
			if (info.getType() == PageType.AD) {
				View v = getActivity().getLayoutInflater().inflate(R.layout.page_ad_item, null);
				container.addView(v);
				return v;
			}

			iv.setImageBitmap(null);

			Bitmap image = getPageBitmap(iv, position);

			if (image != null) {
				int viewH = viewPager.getHeight();
				int imageH = image.getHeight();
				float scale = (float) viewH / imageH;
				if (image.getWidth() > image.getHeight()) {
					// 두장 스캔본
					if (MetricUtils.getOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
						scale = 2.0f;
						iv.setZoom(scale, 0, 0);
					} else {
						scale = 1.0f;
						iv.setZoom(scale, 0.5f, 0);
					}
				} else {
					// 한장 스캔본
					scale = 1.0f;
					iv.setZoom(scale, 0, 0);
				}
				iv.setImageBitmap(image);
			}

			container.addView(iv, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			return iv;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			container.removeView((View) object);
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		private void setupImageView(final TouchImageView iv) {

			iv.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (menuContainer.getVisibility() == View.GONE) {
						menuContainer.setVisibility(View.VISIBLE);
					} else
						menuContainer.setVisibility(View.GONE);

				}
			});

			iv.setOnLongClickListener(new OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					showSettingsActivity();
					return true;
				}
			});

			iv.setOnSideTouchListener(new OnSideTouchListener() {

				@Override
				public void onTouchRightSide() {
					moveToRightPage();
				}

				@Override
				public void onTouchLeftSide() {
					moveToLeftPage();
				}
			});

			iv.setOnTouchListener(new OnTouchListener() {

				private ViewConfiguration viewConfig = ViewConfiguration.get(getActivity());
				private final int scaledTouchSlop = viewConfig.getScaledTouchSlop();
				int startX;
				int startY;

				@Override
				public boolean onTouch(View v, MotionEvent event) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startX = (int) event.getX();
						startY = (int) event.getY();
						break;
					case MotionEvent.ACTION_MOVE:
						if (event.getPointerCount() != 3)
							break;

						// 쓰레기 MOVE모션 걸러냄
						if (Math.hypot((event.getX() - startX), (event.getY() - startY)) > scaledTouchSlop * 2) {
							float slope = ((float) (event.getY() - startY) / Math.abs(event.getX() - startX));
							if (use3Fingers && slope > -2) {
								// 아래로 드래그
								adjustScreenBrightness(false);
							} else if (use3Fingers && slope < 2) {
								// 위로 드래그
								adjustScreenBrightness(true);
							}
						}
						break;
					default:
						break;
					}

					return false;
				}
			});
		}

	}

}
