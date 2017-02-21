package com.yonyou.uap.service.speech;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

//import com.iflytek.cloud.speech.RecognizerResult;
//import com.iflytek.cloud.speech.SpeechConstant;
//import com.iflytek.cloud.speech.SpeechError;
//import com.iflytek.cloud.speech.SpeechListener;
//import com.iflytek.cloud.speech.SpeechSynthesizer;
//import com.iflytek.cloud.speech.SpeechUser;
//import com.iflytek.cloud.speech.SynthesizerListener;
import com.iflytek.cloud.*;
import com.iflytek.*;
import com.iflytek.cloud.speech.*;

import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.yonyou.uap.um.base.UMEventArgs;
import com.yonyou.uap.um.common.Common;
import com.yonyou.uap.um.core.ActionProcessor;
import com.yonyou.uap.um.core.UMActivity;
import com.yonyou.uap.um.runtime.RTHelper;

/**
 * 
 * 
 * @author gct
 * 
 */
public class OpenSpeechService {

	private static SpeechRecognizer mIat;
	private static RecognizerDialog mIatDialog = null;
	
	
	/* 对外服务接口
	 *
	*/
	//对JS开放的接口方法
	public static void init(UMEventArgs args) {

		ApplicationInfo appInfo = null;
		UMActivity act = args.getUMActivity();
		if (Build.VERSION.SDK_INT >= 23) {
          int checkCallPhonePermission = ContextCompat.checkSelfPermission(act, Manifest.permission.RECORD_AUDIO);
          if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
              ActivityCompat.requestPermissions(act, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
          }
		}
			
		
		try {
			appInfo = act.getPackageManager().getApplicationInfo(
					act.getPackageName(), PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		String appid = appInfo.metaData.getString("speech_key_anroid");
		SpeechUtility.createUtility(args.getUMActivity()
				.getApplicationContext(), appid + ","
				+ SpeechConstant.FORCE_LOGIN + "=true");
		
		//实例化一个语音识别对象
		final UMActivity ctx = args.getUMActivity();
		mIat = SpeechRecognizer.createRecognizer(ctx, new InitListener() {

			@Override
			public void onInit(int code) {
				if (code != ErrorCode.SUCCESS) {
					Log.v("gct", "code " + code+","+"SpeechRecognizer.createRecognizer时，init失败!");
				} else {
					Log.v("gct", "code " + code+","+"SpeechRecognizer.createRecognizer时，init成功!");
				}
			}
		});
		/*
		 * SpeechUser.getUser().login(act, null, null, appid, new
		 * SpeechListener() {
		 * 
		 * @Override public void onEvent(int eventType, Bundle params) { // TODO
		 * Auto-generated method stub
		 * 
		 * }
		 * 
		 * public void onData(byte[] buffer) { // TODO Auto-generated method
		 * stub
		 * 
		 * }
		 * 
		 * @Override public void onCompleted(SpeechError error) { // TODO
		 * Auto-generated method stub
		 * 
		 * }
		 * 
		 * @Override public void onBufferReceived(byte[] arg0) { // TODO
		 * Auto-generated method stub
		 * 
		 * } });
		 */
	}
		
		
		
		
	/* 对外服务接口
	 *
	*/
	//对JS开放的接口方法
	public static void openSpeechBackString(UMEventArgs args) {
		exect(args);
	}

	private static void exect(final UMEventArgs args) {
		//1、设置语音识别参数
		setParam(args);

		//2、生成一个语音识别对话类实例
		mIatDialog = new RecognizerDialog(
				args.getUMActivity(), new InitListener() {
					@Override
					public void onInit(int code) {
						if (code != ErrorCode.SUCCESS) {
							Log.v("OpenSpeechService", "RecognizerDialog init success！！result code - " + code);
						}else{
							Log.e("OpenSpeechService", "RecognizerDialog init fail！result code - " + code);
						}
					}
				}
		);
		
		//3、语音识别对话类实例注册回调监听
		mIatDialog.setListener(new RecognizerDialogListener() {
			StringBuffer ret = new StringBuffer();
			
			@Override
			public void onResult(RecognizerResult results, boolean isLast) {
				
				ret.append(JsonParser.parseIatResult(results.getResultString()));
				if (isLast) {
					String cb = args.getString(ActionProcessor.CALLBACK, "");
					if (!Common.isEmpty(cb)) {
						//args.put("text", ret.toString());
						args.put("result", ret.toString());
						RTHelper.execCallBack(args);
						Log.v("OpenSpeechService", "RTHelper.execCallBack！results - " + ret.toString());
					}else{
						Log.v("OpenSpeechService", "no callBack！results - " + ret.toString());
					}
				}
				/*
				 * args.getUMActivity().runOnUiThread(new Runnable() {
				 * 
				 * @Override public void run() { // TODO Auto-generated method
				 * stub mIatDialog.dismiss(); } });
				 */
			}

			/**
			 * 识别回调错误.
			 */
			@Override
			public void onError(SpeechError error) {
				Log.v("gct", "error" + error.toString());
			}

		});
		args.getUMActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mIatDialog.show();
			}
		});
	}

	
	public static void cancleSpeech(UMEventArgs args){
		args.getUMActivity().runOnUiThread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				mIatDialog.dismiss();
			}
		});
	}
	/**
	 * 语音识别参数设置
	 * 
	 * @param param
	 * @return
	 */
	private static void setParam(UMEventArgs args) {
		// 清空参数
		mIat.setParameter(SpeechConstant.PARAMS, null);

		// 设置听写引擎
		mIat.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
		// 设置返回结果格式
		mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

		String language = Common.isEmpty(args.getString("language")) ? "zh_cn" : args.getString("language");;
		// 设置语言
		mIat.setParameter(SpeechConstant.LANGUAGE, language);
		
		// 设置语言区域
		String accent = Common.isEmpty(args.getString("accent")) ? "mandarin" : args.getString("accent");;
		mIat.setParameter(SpeechConstant.ACCENT, accent);
		
		// 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
		mIat.setParameter(SpeechConstant.VAD_BOS, "4000");

		// 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
		mIat.setParameter(SpeechConstant.VAD_EOS, "1000");

		// 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
		mIat.setParameter(SpeechConstant.ASR_PTT, "1");

		// 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
		// 注：AUDIO_FORMAT参数语记需要更新版本才能生效
		mIat.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
		mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH,
				Environment.getExternalStorageDirectory() + "/msc/iat.wav");
	}

	/* 对外服务接口
	 *
	*/
	//对JS开放的接口方法
	public static void openStringBackSpeech(UMEventArgs args) {
		UMActivity ctx = args.getUMActivity();
		String text = args.getString("text");
		setParam(ctx, args);
		setVoice(ctx, args, text);
	}

	private static void setParam(Context context, UMEventArgs args) {
		String voiceName = Common.isEmpty(args.getString("voiceName")) ? "xiaoyan"
				: args.getString("voiceName");
		String speed = Common.isEmpty(args.getString("speed")) ? "50" : args
				.getString("speed");
		String volume = Common.isEmpty(args.getString("volume")) ? "50" : args
				.getString("volume");
		String pitch = Common.isEmpty(args.getString("pitch")) ? "50" : args
				.getString("pitch");

		SpeechSynthesizer speechSynthesizer = SpeechSynthesizer.createSynthesizer(context, null);
		speechSynthesizer.setParameter(SpeechConstant.VOICE_NAME, voiceName);
		speechSynthesizer.setParameter(SpeechConstant.SPEED, speed);
		speechSynthesizer.setParameter(SpeechConstant.VOLUME, volume);
		speechSynthesizer.setParameter(SpeechConstant.PITCH, pitch);
	}

	private static void setVoice(final UMActivity context,
			final UMEventArgs args, String text) {
		SpeechSynthesizer speechSynthesizer = SpeechSynthesizer
				.createSynthesizer(context, null);
		speechSynthesizer.startSpeaking(text, new SynthesizerListener() {

			@Override
			public void onSpeakResumed() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSpeakProgress(int progress, int beginPos, int endPos) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSpeakPaused() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onSpeakBegin() {
				// TODO Auto-generated method stub

			}

			@Override
			public void onCompleted(SpeechError error) {
				String cb = args.getString(ActionProcessor.CALLBACK, "");
				if (Common.isEmpty(cb)) {
					return;
				}
				RTHelper.execCallBack(args);
			}

			@Override
			public void onBufferProgress(int progress, int beginPos,
					int endPos, String info) {
				// TODO Auto-generated method stub

			}

			@Override
			public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
				// TODO Auto-generated method stub

			}
		});

	}

	

	

	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	// 16版本 已经废弃 仅供参考
	public static void openSpeechBackString_old(UMEventArgs args) {
		UMActivity ctx = args.getUMActivity();
		String isSystemUiStr = Common.isEmpty(args.getString("isSystemUi")) ? "true"
				: args.getString("isSystemUi");
		boolean isSystemUi = Boolean.valueOf(isSystemUiStr);
		if (isSystemUi) {
			setDialogListener(ctx, args);
		} else {
			setListener(ctx, args);
		}
	}
	
	private static void setDialogListener(final UMActivity context,
			final UMEventArgs args) {
		RecognizerDialog recognizerDialog = new RecognizerDialog(context, null);
		recognizerDialog.setParameter(SpeechConstant.DOMAIN, "iat");
		recognizerDialog.setParameter(SpeechConstant.SAMPLE_RATE, "16000");
		// 鏄剧ずDialog
		recognizerDialog.setListener(new RecognizerDialogListener() {

			private StringBuffer text = new StringBuffer();

			@Override
			public void onResult(RecognizerResult result, boolean isLast) {
				text.append(JsonParser.parseIatResult(result.getResultString()));

				String cb = args.getString(ActionProcessor.CALLBACK, "");
				if (isLast && !Common.isEmpty(cb)) {
					args.put("text", text);
					RTHelper.execCallBack(args);
				}
			}

			@Override
			public void onError(SpeechError error) {
				// TODO Auto-generated method stub

			}
		});
		recognizerDialog.show();
	}
	
	private static void setListener(final UMActivity context,
			final UMEventArgs args) {
		RecognizerDialog recognizerDialog = new RecognizerDialog(context, null);
		recognizerDialog.setParameter(SpeechConstant.DOMAIN, "iat");
		recognizerDialog.setParameter(SpeechConstant.SAMPLE_RATE, "16000");
		// 鏄剧ずDialog
		recognizerDialog.setListener(new RecognizerDialogListener() {

			@Override
			public void onResult(RecognizerResult result, boolean isLast) {
				String text = JsonParser.parseIatResult(result
						.getResultString());
				String cb = args.getString(ActionProcessor.CALLBACK, "");
				args.put("text", text);
				if (Common.isEmpty(cb)) {
					return;
				}
				RTHelper.execCallBack(args);
			}

			@Override
			public void onError(SpeechError error) {
				// TODO Auto-generated method stub

			}
		});
		recognizerDialog.show();
	}

}
