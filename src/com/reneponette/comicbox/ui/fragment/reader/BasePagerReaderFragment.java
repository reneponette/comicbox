package com.reneponette.comicbox.ui.fragment.reader;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.reneponette.comicbox.R;
import com.reneponette.comicbox.controller.DataController;
import com.reneponette.comicbox.model.FileMeta;
import com.reneponette.comicbox.model.FileMeta.ReadDirection;
import com.reneponette.comicbox.model.PageInfo;
import com.reneponette.comicbox.model.PageInfo.PageType;
import com.reneponette.comicbox.ui.FileSettingsActivity;
import com.reneponette.comicbox.ui.ReaderActivity;
import com.reneponette.comicbox.view.ExtendedViewPager;
import com.reneponette.comicbox.view.TouchImageView;
import com.reneponette.comicbox.view.TouchImageView.OnSideTouchListener;

@SuppressWarnings("deprecation")
public class BasePagerReaderFragment extends BaseReaderFragment {

	protected ExtendedViewPager viewPager;
	protected PagerAdapter pagerAdapter;

	protected Gallery previewGallery;
	protected PreviewAdapter previewAdapter;
	protected View menuContainer;
	protected TextView filename;
	protected SeekBar seekBar;

	private boolean use3Fingers;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pagerAdapter = new TouchImageAdapter(dataController);
		previewAdapter = new PreviewAdapter(dataController);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

		seekBar = (SeekBar) rootView.findViewById(R.id.seekBar1);
		seekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {

			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser)
					previewGallery.setSelection(progress);
			}
		});

		previewGallery = (Gallery) rootView.findViewById(R.id.preview_selector);
		previewGallery.setAdapter(previewAdapter);
		previewGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				viewPager.setCurrentItem(position);
				seekBar.setProgress(position);
			}
		});

		return rootView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		viewPager.setOnPageChangeListener(new OnPageChangeListener() {

			@Override
			public void onPageSelected(int position) {
				previewGallery.setSelection(position);
				seekBar.setProgress(position);

				PageInfo pi = dataController.getPageInfo(position);
				filename.setText(pi.getName());

				checkEndPage(position);
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
	}

	@Override
	public void onDestroy() {
		dataController.setOnDataBuildListener(null);
		dataController.stopBuilding();
		dataController.saveReadState(viewPager.getCurrentItem());
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

			Intent intent = ReaderActivity.newIntent(getActivity(), dataController.getFileInfo());
			startActivity(intent);
			return;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	/*---------------------------------------------------------------------------*/

	private void checkEndPage(int position) {
		boolean isEndPage = false;
		if (dataController.getReadDirection() == ReadDirection.RTL) {
			isEndPage = position == 0;
		} else {
			isEndPage = position == dataController.pageSize() - 1;
		}

		if (isEndPage)
			onGoNextFile();
	}

	protected void onGoNextFile() {

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
		viewPager.setCurrentItem(curIndex);
	}

	@Override
	protected void onMoveToRightPage() {
		int curIndex = viewPager.getCurrentItem();
		curIndex++;
		if (curIndex >= pagerAdapter.getCount()) {
			curIndex = pagerAdapter.getCount() - 1;
		}
		viewPager.setCurrentItem(curIndex);
	}

	@Override
	public void onResume() {
		super.onResume();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
		use3Fingers = settings.getBoolean("viewer_use_3_fingers", false);
	}

	@Override
	public boolean onBackPressed() {
		if (menuContainer.getVisibility() == View.VISIBLE) {
			menuContainer.setVisibility(View.GONE);
			return true;
		}
		return false;
	}

	private void showSettingsActivity() {
		// 설정 액티비티 띠우기
		dataController.saveReadState(viewPager.getCurrentItem());
		startActivityForResult(FileSettingsActivity.newIntent(getActivity(), dataController.getFileInfo()),
				REQ_SETTINGS);
	}

	protected void initUI() {
		FileMeta meta = dataController.getFileInfo().getMeta();
		ReadDirection readDirection = dataController.getReadDirection();
		int pagesPerScanComputed = dataController.getPagesPerScan();
		int pageSize = dataController.pageSize();

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
		if (dataController.getReadDirection() == ReadDirection.RTL) {
			((TextView) getView().findViewById(R.id.pageRight)).setText("1");
			((TextView) getView().findViewById(R.id.pageLeft)).setText(dataController.pageSize() + "");
		} else {
			((TextView) getView().findViewById(R.id.pageLeft)).setText("1");
			((TextView) getView().findViewById(R.id.pageRight)).setText(dataController.pageSize() + "");
		}

	}

	/*------------------------------------------------------------------*/

	private class TouchImageAdapter extends PagerAdapter {

		DataController controller;

		public TouchImageAdapter(DataController controller) {
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

			Bitmap image = getPageBitmap(iv, position);

			if (image != null) {
				int viewH = viewPager.getHeight();
				int imageH = image.getHeight();
				float scale = (float) viewH / imageH;
				if (image.getWidth() > image.getHeight()) {
					// 두장 스캔본
					scale = 2.0f;
				} else {
					// 한장 스캔본
					scale = 1.0f;
				}
				iv.setZoom(scale, 0, 0.5f);
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

		/*------------Adapters-----------------------------------------------*/

		private void setupImageView(final TouchImageView iv) {

			iv.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					int curIndex = viewPager.getCurrentItem();
					if (menuContainer.getVisibility() == View.GONE) {
						menuContainer.setVisibility(View.VISIBLE);
						previewGallery.setSelection(curIndex);
					} else
						menuContainer.setVisibility(View.GONE);
					previewAdapter.notifyDataSetChanged();

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

	public class PreviewAdapter extends BaseAdapter {

		DataController controller;

		public PreviewAdapter(DataController controller) {
			this.controller = controller;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = getActivity().getLayoutInflater().inflate(R.layout.preview_item, previewGallery, false);
				Holder holder = new Holder();
				holder.previewIv = (ImageView) view.findViewById(R.id.previewImage);
				holder.pageNumTv = (TextView) view.findViewById(R.id.pageNumber);
				view.setTag(holder);
			}

			final Holder holder = (Holder) view.getTag();

			if (controller.getReadDirection() == ReadDirection.RTL) {
				holder.pageNumTv.setText((controller.pageSize() - position) + "");
			} else {
				holder.pageNumTv.setText(position + 1 + "");
			}

			PageInfo pi = (PageInfo) getItem(position);
			if (pi == null) {
				return view;
			}

			if (pi.getType() == PageType.END) {
				return new ImageView(getActivity());
			}

			Bitmap previewBitmap = getPreviewBitmap(holder.previewIv, position);
			if (previewBitmap != null)
				holder.previewIv.setImageBitmap(previewBitmap);

			return view;
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public Object getItem(int position) {
			if (position >= controller.pageSize())
				return null;

			return controller.getPageInfo(position);
		}

		@Override
		public int getCount() {
			return controller.pageSize();
		}

		class Holder {
			public ImageView previewIv;
			public TextView pageNumTv;
		}
	}

}
