//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.library"
package plugin.branch;

import io.branch.referral.Branch;
import io.branch.referral.Branch.BranchReferralInitListener;
import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralStateChangedListener;
import io.branch.referral.Branch.BranchListResponseListener;

import io.branch.referral.BranchError;

import org.json.JSONObject;
import org.json.JSONArray;

import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.HashMap;
import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTaskDispatcher;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;


/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	public LuaLoader() {
		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called everytime a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 *         <p>
	 *         Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
			new InitWrapper(),
			new CallWrapper(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.
	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
		Branch.getInstance(CoronaEnvironment.getApplicationContext()).closeSession();
	}

	/**
	 * The following Lua function has been called:  library.init( listener )
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param L Reference to the Lua state that the Lua function was called from.
	 * @return Returns the number of values to be returned by the library.init() function.
	 */
	public int init(final LuaState L) {
		final String app_key = L.checkString( 1 );

		final int luaFunctionStackIndex = 2;
		try {
			L.checkType(luaFunctionStackIndex, com.naef.jnlua.LuaType.FUNCTION);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return 0;
		}
		L.pushValue(luaFunctionStackIndex);

		final int luaFunctionReferenceKey = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);
		final com.ansca.corona.CoronaRuntimeTaskDispatcher dispatcher =
					new com.ansca.corona.CoronaRuntimeTaskDispatcher(L);

		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			return 0;
		}
		try {
			activity.runOnUiThread(new Runnable() {

				public void run() {
					// Fetch a reference to the Corona activity.
					// Note: Will be null if the end-user has just backed out of the activity.
						CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
						if (activity == null) {
							return ;
						}

					 Branch branch = Branch.getInstance(activity,app_key);
					 branch.initSession(new BranchReferralInitListener() {
					 	@Override
					 	public void onInitFinished(JSONObject referringParams, BranchError error) {

							try {

								CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
								if (activity == null) {
									return;
								}

								activity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										// *** We are now running in the main UI thread. ***

										// Create a task that will call the given Lua function.
										// This task's execute() method will be called on the Corona runtime thread, just before rendering a frame.
										com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
											@Override
											public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
												// *** We are now running on the Corona runtime thread. ***
												try {
													// Fetch the Corona runtime's Lua state.
													com.naef.jnlua.LuaState luaState = runtime.getLuaState();

													// Fetch the Lua function stored in the registry and push it to the top of the stack.
													luaState.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, luaFunctionReferenceKey);

													// Remove the Lua function from the registry.
													luaState.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, luaFunctionReferenceKey);

													luaState.newTable(0, 1);
													int luaTableStackIndex = L.getTop();
													luaState.pushString("ready");
													luaState.setField(luaTableStackIndex, "state");

													// Call the Lua function that was just pushed to the top of the stack.
													// The 1st argument indicates the number of arguments that we are passing to the Lua function.
													// The 2nd argument indicates the number of return values to accept from the Lua function.
													// In this case, we are calling this Lua function without arguments and accepting no return values.
													// Note: If you want to call the Lua function with arguments, then you need to push each argument
													//       value to the luaState object's stack.
													luaState.call(1, 0);
												}
												catch (Exception ex) {
													ex.printStackTrace();
												}
											}
										};

										// Send the above task to the Corona runtime asynchronously.
										// The send() method will do nothing if the Corona runtime is no longer available, which can
										// happen if the runtime was disposed/destroyed after the user has exited the Corona activity.
										dispatcher.send(task);
									}
								});

							}
							catch (Exception ex) {
								ex.printStackTrace();
								// return 0;
							}

					 	}
					 }, activity.getIntent().getData(), activity);
				}
			});
		}
		catch( Exception ex )
    {
        // An exception will occur if given an invalid argument or no argument. Print the error.
        ex.printStackTrace();
    }

		return 0;
	}

	private HashMap luaTabletoHashMap(com.naef.jnlua.LuaState L, int luaTableStackIndex) {
		HashMap hashmapobj = new HashMap();

		for (L.pushNil(); L.next(luaTableStackIndex); L.pop(1)) {
				// Fetch the table entry's string key.
				// An index of -2 accesses the key that was pushed into the Lua stack by L.next() up above.
				String keyName = null;
				com.naef.jnlua.LuaType luaType = L.type(-2);
				switch (luaType) {
					case STRING:
						// Fetch the table entry's string key.
						keyName = L.toString(-2);
						break;
					case NUMBER:
						// The key will be a number if the given Lua table is really an array.
						// In this case, the key is an array index. Do not call L.toString() on the
						// numeric key or else Lua will convert the key to a string from within the Lua table.
						keyName = Integer.toString(L.toInteger(-2));
						break;
				}
				if (keyName == null) {
					// A valid key was not found. Skip this table entry.
					continue;
				}

				// Fetch the table entry's value in string form.
				// An index of -1 accesses the entry's value that was pushed into the Lua stack by L.next() above.
				String valueString = null;
				HashMap valueHash = new HashMap();
				luaType = L.type(-1);
				switch (luaType) {
					case STRING:
						valueString = L.toString(-1);
						break;
					case BOOLEAN:
						valueString = Boolean.toString(L.toBoolean(-1));
						break;
					case NUMBER:
						valueString = Double.toString(L.toNumber(-1));
						break;
					case TABLE:
						valueHash = luaTabletoHashMap(L, -2);
						break;
					default:
						valueString = luaType.displayText();
						break;
				}
				if (valueString == null) {
					valueString = "";
				}
				hashmapobj.put(keyName, valueString);
				if(!valueHash.isEmpty()){
					hashmapobj.put(keyName, valueHash);
				}
		}

		return hashmapobj;
	}

	/**
	 * The following Lua function has been called:  library.show( word )
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param L Reference to the Lua state that the Lua function was called from.
	 * @return Returns the number of values to be returned by the library.show() function.
	 */
	public int callFunc(final LuaState L) {

		final int luaTableStackIndex = 1;
		L.checkType(luaTableStackIndex, com.naef.jnlua.LuaType.TABLE);

		// get command
		L.getField(luaTableStackIndex, "command");
		final String command = L.toString(-1);
		// L.pop(1);

		// get params
		HashMap params = new HashMap();
		L.getField(luaTableStackIndex, "params");
		try {
			L.checkType(-1, com.naef.jnlua.LuaType.TABLE);
			L.pushValue(-1);
			params = luaTabletoHashMap(L,-2);
		}
		catch (Exception ex) {
		}

		// get response method
		L.getField(luaTableStackIndex, "response");
		try {
			L.checkType(-1, com.naef.jnlua.LuaType.FUNCTION);
			L.pushValue(-1);
		}
		catch (Exception ex) {
			// ex.printStackTrace();
			// return 0;
		}

		final int luaFunctionReferenceKey = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);
		final com.ansca.corona.CoronaRuntimeTaskDispatcher dispatcher =
					new com.ansca.corona.CoronaRuntimeTaskDispatcher(L);

		L.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, luaFunctionReferenceKey);

		// Remove the Lua function from the registry.
		L.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, luaFunctionReferenceKey);

		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			return 0;
		}

		final Branch branch = Branch.getInstance(activity);

		try {
			if(command.equals("getLatestReferringParams")){

				JSONObject sessionParams = branch.getLatestReferringParams();

				L.newTable(0, 1);
				int tableTop = L.getTop();
				L.pushString(sessionParams.toString());
				L.setField(tableTop, "data");

				L.call(1, 0);

			} else if(command.equals("getFirstReferringParams")){
				JSONObject sessionParams = branch.getFirstReferringParams();

				L.newTable(0, 1);
				int tableTop = L.getTop();
				L.pushString(sessionParams.toString());
				L.setField(tableTop, "data");

				L.call(1, 0);
			} else if(command.equals("setIdentity")){
				branch.setIdentity(params.get("id").toString());
			} else if(command.equals("logout")){
				branch.logout();
			} else if(command.equals("userCompletedAction")){
				JSONObject appState=new JSONObject( (HashMap)params.get("appState"));
				branch.userCompletedAction(params.get("event").toString(), appState);
			} else if(command.equals("getShortURLWithParams")){
				JSONObject data = new JSONObject( (HashMap)params.get("params"));
				HashMap tags = (HashMap)params.get("andTags");

				final int getURLCallbackRef = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);

				branch.getShortUrl(params.get("andAlias").toString(), tags.values(), params.get("andChannel").toString(), params.get("andFeature").toString(), params.get("andStage").toString(), data, new BranchLinkCreateListener() {
			    @Override
			    public void onLinkCreate(final String url, final BranchError error) {

						try {

							CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
							if (activity == null) {
								return;
							}

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
										@Override
										public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
											try {
												com.naef.jnlua.LuaState L = runtime.getLuaState();
												L.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, getURLCallbackRef);
												L.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, getURLCallbackRef);
												if (error == null) {
														L.newTable(0, 1);
														int tableTop = L.getTop();
														L.pushString(url);
														L.setField(tableTop, "data");
														L.call(1, 0);
								        } else {
													// Log.i("Corona", error.getMessage());
													L.newTable(0, 1);
													int tableTop = L.getTop();
													L.pushString(error.getMessage().toString());
													L.setField(tableTop, "error");
													L.call(1, 0);
								        }
											}
											catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									};
									dispatcher.send(task);
								}
							});

						}
						catch (Exception ex) {
							ex.printStackTrace();
							// return 0;
						}


    			}
				});
			} else if(command.equals("loadRewardsWithCallback")){

				final int loadRewardsCallbackRef = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);

				branch.loadRewards(new BranchReferralStateChangedListener() {
			    @Override
			    public void onStateChanged(final boolean changed, final BranchError error) {
						try {

							CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
							if (activity == null) {
								return;
							}

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
										@Override
										public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
											try {
												com.naef.jnlua.LuaState L = runtime.getLuaState();
												L.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, loadRewardsCallbackRef);
												L.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, loadRewardsCallbackRef);
												if (error == null) {
														L.newTable(0, 1);
														int tableTop = L.getTop();
														L.pushNumber(branch.getCredits());
														L.setField(tableTop, "credits");
														L.call(1, 0);
								        } else {
													L.newTable(0, 1);
													int tableTop = L.getTop();
													L.pushString(error.getMessage().toString());
													L.setField(tableTop, "error");
													L.call(1, 0);
								        }
											}
											catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									};
									dispatcher.send(task);
								}
							});

						}
						catch (Exception ex) {
							ex.printStackTrace();
							// return 0;
						}


    			}
				});
			} else if(command.equals("redeemRewards")){
				Double credits = Double.parseDouble(params.get("credits").toString());
				branch.redeemRewards(credits.intValue());
			} else if(command.equals("getCreditHistoryWithCallback")){

				final int loadCreditHistoryCallbackRef = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);

				branch.getCreditHistory(new BranchListResponseListener() {
    			public void onReceivingResponse(final JSONArray list, final BranchError error) {
						try {

							CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
							if (activity == null) {
								return;
							}

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
										@Override
										public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
											try {
												com.naef.jnlua.LuaState L = runtime.getLuaState();
												L.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, loadCreditHistoryCallbackRef);
												L.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, loadCreditHistoryCallbackRef);
												if (error == null) {
														L.newTable(0, 1);
														int tableTop = L.getTop();
														L.pushString(list.toString());
														L.setField(tableTop, "history");
														L.call(1, 0);
								        } else {
													L.newTable(0, 1);
													int tableTop = L.getTop();
													L.pushString(error.getMessage().toString());
													L.setField(tableTop, "error");
													L.call(1, 0);
								        }
											}
											catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									};
									dispatcher.send(task);
								}
							});

						}
						catch (Exception ex) {
							ex.printStackTrace();
							// return 0;
						}


    			}
				});
			} else if(command.equals("getReferralCodeWithCallback") || command.equals("getReferralCodeWithAmount") || command.equals("getReferralCodeWithPrefix")){

				final int getReferralCallbackRef = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);

				BranchReferralInitListener branchReferralListener = new BranchReferralInitListener(){
					@Override
			    public void onInitFinished(final JSONObject referralCode, final BranchError error) {
						try {

							CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
							if (activity == null) {
								return;
							}

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
										@Override
										public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
											try {
												com.naef.jnlua.LuaState L = runtime.getLuaState();
												L.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, getReferralCallbackRef);
												L.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, getReferralCallbackRef);
												if (error == null) {
														L.newTable(0, 1);
														int tableTop = L.getTop();
														L.pushString(referralCode.toString());
														L.setField(tableTop, "referral_code");
														L.call(1, 0);
								        } else {
													L.newTable(0, 1);
													int tableTop = L.getTop();
													L.pushString(error.getMessage().toString());
													L.setField(tableTop, "error");
													L.call(1, 0);
								        }
											}
											catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									};
									dispatcher.send(task);
								}
							});

						}
						catch (Exception ex) {
							ex.printStackTrace();
							// return 0;
						}
					}
				};

				if(command.equals("getReferralCodeWithAmount")){
					Double amount = Double.parseDouble(params.get("amount").toString());
					branch.getReferralCode(amount.intValue(), branchReferralListener);
				} else if(command.equals("getReferralCodeWithPrefix")){
					Double amount = Double.parseDouble(params.get("amount").toString());
					Date now = new Date();
					Double seconds = Double.parseDouble(params.get("expiration").toString());
         	now.setSeconds(now.getSeconds() + seconds.intValue());
					Integer calculationType = 0;
					Integer location = 0;
					if(params.get("calculationType") != null) {
						Double calcType = Double.parseDouble(params.get("calculationType").toString());
						calculationType = calcType.intValue();
					}
					if(params.get("location") != null) {
						Double loc = Double.parseDouble(params.get("location").toString());
						location = loc.intValue();
					}
					branch.getReferralCode( params.get("prefix").toString(),
																 	amount.intValue(),
																	now,
																	params.get("bucket").toString(),
																	calculationType,
																	location,
																	branchReferralListener);
				} else {
					branch.getReferralCode(branchReferralListener);
				}
			} else if(command.equals("validateReferralCode") || command.equals("applyReferralCode")){

				final int applyReferralCallbackRef = L.ref(com.naef.jnlua.LuaState.REGISTRYINDEX);

				BranchReferralInitListener branchReferralListener = new BranchReferralInitListener(){
					@Override
			    public void onInitFinished(final JSONObject referralCode, final BranchError error) {
						try {

							CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
							if (activity == null) {
								return;
							}

							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									com.ansca.corona.CoronaRuntimeTask task = new com.ansca.corona.CoronaRuntimeTask() {
										@Override
										public void executeUsing(com.ansca.corona.CoronaRuntime runtime) {
											try {
												com.naef.jnlua.LuaState L = runtime.getLuaState();
												L.rawGet(com.naef.jnlua.LuaState.REGISTRYINDEX, applyReferralCallbackRef);
												L.unref(com.naef.jnlua.LuaState.REGISTRYINDEX, applyReferralCallbackRef);
												if (error == null) {
														L.newTable(0, 1);
														int tableTop = L.getTop();
														L.pushBoolean(true);
														L.setField(tableTop, "valid");
														L.call(1, 0);
								        } else {
													L.newTable(0, 1);
													int tableTop = L.getTop();
													L.pushString(error.getMessage().toString());
													L.setField(tableTop, "error");
													L.call(1, 0);
								        }
											}
											catch (Exception ex) {
												ex.printStackTrace();
											}
										}
									};
									dispatcher.send(task);
								}
							});

						}
						catch (Exception ex) {
							ex.printStackTrace();
							// return 0;
						}
					}
				};

				if(command.equals("validateReferralCode")){
					branch.validateReferralCode(params.get("code").toString(), branchReferralListener);
				} else if(command.equals("applyReferralCode")){
					branch.applyReferralCode(params.get("code").toString(), branchReferralListener);
				}
			}

		} catch (Exception ex){

			CharArrayWriter cw = new CharArrayWriter();
    	PrintWriter w = new PrintWriter(cw);
			ex.printStackTrace(w);
    	w.close();
    	String trace = cw.toString();

			Log.i("Corona",trace);

			L.newTable(0, 1);
			int tableTop = L.getTop();
			L.pushString(trace);
			L.setField(tableTop, "error");

			L.call(1, 0);
		}

		return 0;
	}

	/** Implements the library.init() Lua function. */
	private class InitWrapper implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "init";
		}

		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param luaState Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {
			return init(L);
		}
	}

	/** Implements the library.call() Lua function. */
	private class CallWrapper implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "call";
		}

		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param luaState Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {
			return callFunc(L);
		}
	}
}
