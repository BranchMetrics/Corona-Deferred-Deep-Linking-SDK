//
//  PluginLibrary.mm
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#import "PluginBranch.h"

#include "CoronaRuntime.h"

#import <UIKit/UIKit.h>
#import "Branch-SDK/Branch.h"
#import "NotificationChecker.h"

// ----------------------------------------------------------------------------

class PluginBranch
{
	public:
		typedef PluginBranch Self;

	public:
		static const char kName[];
		static const char kEvent[];
        NSMutableDictionary *launchOptions;
    
	protected:
		PluginBranch();

	public:
		bool Initialize( CoronaLuaRef listener );

	public:
		CoronaLuaRef GetListener() const { return fListener; }

	public:
		static int Open( lua_State *L );

	protected:
		static int Finalizer( lua_State *L );

	public:
		static Self *ToLibrary( lua_State *L );

	public:
		static int call( lua_State *L );
        static int init( lua_State *L );
        static NSMutableDictionary *luaTableToDictionary (lua_State* L,int stack_index);
        static Branch *branch;

	private:
		CoronaLuaRef fListener;
};

// ----------------------------------------------------------------------------

// This corresponds to the name of the library, e.g. [Lua] require "plugin.library"
const char PluginBranch::kName[] = "plugin.branch";

// This corresponds to the event name, e.g. [Lua] event.name
const char PluginBranch::kEvent[] = "branchevent";

PluginBranch::PluginBranch()
:	fListener( NULL )
{
}

Branch *PluginBranch::branch;

NSMutableDictionary* PluginBranch::luaTableToDictionary(lua_State* L,int stack_index)
{
    NSMutableDictionary *dict = [NSMutableDictionary dictionary];
    
    //  STACK  { Table }
    
    lua_pushnil(L);
    //  STACK  { Table      Nil     }
    
    
    while (lua_next(L, stack_index) != 0) {
        
        //  STACK  { Table      key     Value   }
        
        
        id key = nil;
        switch(lua_type(L,-2))  {
            case LUA_TNUMBER: {
                int value = lua_tonumber(L, -2);
                NSNumber *number = [NSNumber numberWithInt:value];
                key = number;
                break;
            }
            case LUA_TSTRING: {
                NSString *value = [NSString stringWithUTF8String:luaL_checkstring(L, -2)];
                key = value;
                break;
            }
                
        }
        
        
        id value = nil;
        switch (lua_type(L, -1)) {
            case LUA_TNUMBER: {
                int val = lua_tonumber(L, -1);
                NSNumber *number = [NSNumber numberWithInt:val];
                value = number;
                break;
            }
            case LUA_TBOOLEAN: {
                int val = lua_toboolean(L, -1);
                NSNumber *number = [NSNumber numberWithBool:val];
                value = number;
                break;
            }
            case LUA_TSTRING: {
                NSString *val = [NSString stringWithUTF8String:luaL_checkstring(L, -1)];
                value = val;
                break;
            }
            case LUA_TTABLE: {
                NSMutableDictionary *dict = luaTableToDictionary(L,stack_index+2);
                value = dict;
                break;
            }
            default : {
                lua_pop(L, 1);
                continue;
            }
        }
        if(value == nil) {
            value = @"";
        }
        [dict setObject:value forKey:key];
        lua_pop(L, 1);
        
    }
    
    
    return dict;
}

bool
PluginBranch::Initialize( CoronaLuaRef listener )
{
	// Can only initialize listener once
	bool result = ( NULL == fListener );

	if ( result )
	{
		fListener = listener;
	}

	return result;
}

int
PluginBranch::Open( lua_State *L )
{
	// Register __gc callback
	const char kMetatableName[] = __FILE__; // Globally unique string to prevent collision
	CoronaLuaInitializeGCMetatable( L, kMetatableName, Finalizer );

	// Functions in library
	const luaL_Reg kVTable[] =
	{
		{ "init", init },
        { "call", call },

		{ NULL, NULL }
	};

	// Set library as upvalue for each library function
	Self *library = new Self;
	CoronaLuaPushUserdata( L, library, kMetatableName );

	luaL_openlib( L, kName, kVTable, 1 ); // leave "library" on top of stack

	return 1;
}

int
PluginBranch::Finalizer( lua_State *L )
{
	Self *library = (Self *)CoronaLuaToUserdata( L, 1 );

	CoronaLuaDeleteRef( L, library->GetListener() );

	delete library;

	return 0;
}

PluginBranch *
PluginBranch::ToLibrary( lua_State *L )
{
	// library is pushed as part of the closure
	Self *library = (Self *)CoronaLuaToUserdata( L, lua_upvalueindex( 1 ) );
	return library;
}

int
PluginBranch::init( lua_State *L )
{
    int listenerIndex = 2;
    const char *ak = lua_tostring( L, 1 );
    
    NSString *app_key = [NSString stringWithFormat:@"%s", ak];
    
    if ( CoronaLuaIsListener( L, listenerIndex, kEvent ) )
    {
        Self *library = ToLibrary( L );
        
        CoronaLuaRef listener = CoronaLuaNewRef( L, listenerIndex );
        library->Initialize( listener );
        
//        [Branch setDebug];
        branch = [Branch getInstance:app_key];
        
        [branch initSessionWithLaunchOptions:NotificationChecker.launchOptions andRegisterDeepLinkHandler:^(NSDictionary *params, NSError *error) {
            if(!error) {
                CoronaLuaNewEvent( L, kEvent );
                lua_pushstring( L, [@"ready" UTF8String] );
                lua_setfield( L, -2, "state" );
                
                // Dispatch event to library's listener
                CoronaLuaDispatchEvent( L, library->GetListener(), 0 );
            } else {
                NSLog(@"%@",[error description]);
            }
        }];
    }

    return 0;
}

int
PluginBranch::call( lua_State *L )
{
    // command, params, callback
    
    // The app id
    const char *command;
    CoronaLuaRef listener;
    NSMutableDictionary *params;
    
    Self *library = ToLibrary( L );
    
    // If an options table has been passed
    if ( lua_type( L, -1 ) == LUA_TTABLE )
    {
        // Get listener key
        lua_getfield( L, -1, "response" );
        if ( CoronaLuaIsListener( L, -1, kEvent ) )
        {
            listener = CoronaLuaNewRef( L, -1 );
        }

        lua_pop( L, 1 );
        
        // Get the command
        lua_getfield( L, -1, "command" );
        if ( lua_type( L, -1 ) == LUA_TSTRING )
        {
            command = lua_tostring( L, -1 );
        }
        else
        {
            luaL_error( L, "Error: command expected, got: %s", luaL_typename( L, -1 ) );
        }
        lua_pop( L, 1 );
        
        // Get the params
        lua_getfield( L, -1, "params" );
        if ( lua_type( L, -1 ) == LUA_TTABLE )
        {
            params = library->luaTableToDictionary(L,2);
        }
        else
        {
            params = [NSMutableDictionary dictionary];
        }
        lua_pop( L, 1 );
        
        NSString *command_string = [[NSString alloc] initWithUTF8String:command];
        
        if([command_string isEqualToString: @"getLatestReferringParams"]) {
            NSDictionary *sessionParams = [branch getLatestReferringParams];
            NSError *error = nil;
            NSString *jsonData = [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:sessionParams options:0 error:&error] encoding:NSUTF8StringEncoding];
            if(listener) {

                if(!error){
                    CoronaLuaNewEvent( L, "command" );
                    lua_pushstring( L, [jsonData UTF8String] );
                    lua_setfield( L, -2, "data" );
                }else{
                    CoronaLuaNewEvent( L, "command" );
                    lua_pushstring( L, [[error description] UTF8String] );
                    lua_setfield( L, -2, "error" );
                }
                CoronaLuaDispatchEvent( L, listener, 0 );
            }
        } else if([command_string isEqualToString: @"getFirstReferringParams"]) {
            NSDictionary *sessionParams = [branch getFirstReferringParams];
            NSError *error = nil;
            NSString *jsonData = [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:sessionParams options:0 error:&error] encoding:NSUTF8StringEncoding];
            if(listener) {
                if(!error){
                    CoronaLuaNewEvent( L, "command" );
                    lua_pushstring( L, [jsonData UTF8String] );
                    lua_setfield( L, -2, "data" );
                }else{
                    CoronaLuaNewEvent( L, "command" );
                    lua_pushstring( L, [[error description] UTF8String] );
                    lua_setfield( L, -2, "error" );
                }
                CoronaLuaDispatchEvent( L, listener, 0 );
            }
        } else if([command_string isEqualToString: @"setIdentity"]) {
            [branch setIdentity:[params objectForKey:@"id"]];
        } else if([command_string isEqualToString: @"logout"]) {
            [branch logout];
        } else if([command_string isEqualToString: @"userCompletedAction"]) {
            [branch userCompletedAction:[params objectForKey:@"event"] withState:[params objectForKey:@"appState"]];
        } else if([command_string isEqualToString: @"getShortURLWithParams"]) {
            [branch getShortURLWithParams:[params objectForKey:@"params"] andTags:[[params objectForKey:@"andTags"] allValues] andChannel:[params objectForKey:@"andChannel"] andFeature:[params objectForKey:@"andFeature"] andStage:[params objectForKey:@"andStage"] andAlias:[params objectForKey:@"andAlias"] andCallback:^(NSString *url, NSError *error) {
                if(listener) {
                    if(!error){
                        CoronaLuaNewEvent( L, "command" );
                        lua_pushstring( L, [url UTF8String] );
                        lua_setfield( L, -2, "url" );
                    }else{
                        CoronaLuaNewEvent( L, "command" );;
                        lua_pushstring( L, [[error description] UTF8String] );
                        lua_setfield( L, -2, "error" );
                    }
                    CoronaLuaDispatchEvent( L, listener, 0 );
                }
            }];
            
        } else if([command_string isEqualToString: @"loadRewardsWithCallback"]) {
            [branch loadRewardsWithCallback:^(BOOL changed, NSError *error) {
                NSInteger credits = [branch getCredits];
                if(!error){
                    CoronaLuaNewEvent( L, "command" );
                    lua_pushinteger( L, (int)credits );
                    lua_setfield( L, -2, "credits" );
                }else{
                    CoronaLuaNewEvent( L, "command" );
                    lua_pushstring( L, [[error description] UTF8String] );
                    lua_setfield( L, -2, "error" );
                }
                CoronaLuaDispatchEvent( L, listener, 0 );
                
            }];
        } else if([command_string isEqualToString: @"redeemRewards"]) {
            [branch redeemRewards:[[params objectForKey:@"reward"] intValue]];
        } else if([command_string isEqualToString: @"getCreditHistoryWithCallback"]) {
            [branch getCreditHistoryWithCallback:^(NSArray *history, NSError *error) {
                if (!error) {
                    NSString *jsonData = [[NSString alloc] initWithData:[NSJSONSerialization dataWithJSONObject:history options:0 error:&error] encoding:NSUTF8StringEncoding];
                    if(listener) {
                        if(!error){
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushstring( L, [jsonData UTF8String] );
                            lua_setfield( L, -2, "history" );
                        }else{
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushstring( L, [[error description] UTF8String] );
                            lua_setfield( L, -2, "error" );
                        }
                        CoronaLuaDispatchEvent( L, listener, 0 );
                    }
                    
                }
            }];
        } else if([command_string isEqualToString: @"getReferralCodeWithCallback"]) {
            [branch getReferralCodeWithCallback:^(NSDictionary *params, NSError *error) {
                if(listener) {
                    if (!error) {
                        NSString *referralCode = [params objectForKey:@"referral_code"];
                        CoronaLuaNewEvent( L, "command" );
                        lua_pushstring( L, [referralCode UTF8String] );
                        lua_setfield( L, -2, "referral_code" );
                        CoronaLuaDispatchEvent( L, listener, 0 );
                    } else {
                        CoronaLuaNewEvent( L, "command" );
                        lua_pushstring( L, [[error description] UTF8String] );
                        lua_setfield( L, -2, "error" );
                        CoronaLuaDispatchEvent( L, listener, 0 );
                    }
                }
            }];
        } else if([command_string isEqualToString: @"getReferralCodeWithAmount"]) {
            [branch getReferralCodeWithAmount: [[params objectForKey:@"amount"] intValue]
                andCallback:^(NSDictionary *params, NSError *error) {
                    if(listener) {
                        if (!error) {
                            NSString *referralCode = [params objectForKey:@"referral_code"];
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushstring( L, [referralCode UTF8String] );
                            lua_setfield( L, -2, "referral_code" );
                            CoronaLuaDispatchEvent( L, listener, 0 );
                        } else {
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushstring( L, [[error description] UTF8String] );
                            lua_setfield( L, -2, "error" );
                            CoronaLuaDispatchEvent( L, listener, 0 );
                        }
                    }
                }
             ];
        } else if([command_string isEqualToString: @"getReferralCodeWithPrefix"]) {
            ReferralCodeCalculation calculationType;
            switch([[params objectForKey:@"calculationType"] intValue]){
                case 0:
                    calculationType = BranchUnlimitedRewards;
                    break;
                case 1:
                    calculationType = BranchUniqueRewards;
                    break;
                default:
                    calculationType = BranchUnlimitedRewards;
                    break;
            }
            ReferralCodeLocation locationType;
            switch([[params objectForKey:@"location"] intValue]){
                case 0:
                    locationType = BranchReferreeUser;
                    break;
                case 2:
                    locationType = BranchReferringUser;
                    break;
                case 3:
                    locationType = BranchBothUsers;
                    break;
                default:
                    locationType = BranchReferringUser;
                    break;
            }
            
            [branch getReferralCodeWithPrefix:[params objectForKey:@"prefix"]
                amount:[[params objectForKey:@"amount"] intValue]
                expiration:[[NSDate date] dateByAddingTimeInterval:[[params objectForKey:@"expiration"] intValue]]
                bucket:[params objectForKey:@"bucket"]
                calculationType:calculationType
                location:locationType
                andCallback:^(NSDictionary *params, NSError *error) {
                    if(listener) {
                        if (!error) {
                            NSString *referralCode = [params objectForKey:@"referral_code"];
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushstring( L, [referralCode UTF8String] );
                            lua_setfield( L, -2, "referral_code" );
                            CoronaLuaDispatchEvent( L, listener, 0 );
                        } else {
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushstring( L, [[error description] UTF8String] );
                            lua_setfield( L, -2, "error" );
                            CoronaLuaDispatchEvent( L, listener, 0 );
                        }
                    }
                }
             ];
        } else if([command_string isEqualToString: @"validateReferralCode"]) {
            [branch validateReferralCode:[params objectForKey:@"code"] andCallback:^(NSDictionary *params, NSError *error) {
                if (!error) {
                    if ([[params objectForKey:@"code"] isEqualToString:[params objectForKey:@"referral_code"]]) {
                        if(listener) {
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushboolean( L, YES );
                            lua_setfield( L, -2, "valid" );
                            CoronaLuaDispatchEvent( L, listener, 0 );
                        }
                    } else {
                        if(listener) {
                            CoronaLuaNewEvent( L, "command" );
                            lua_pushboolean( L, NO );
                            lua_setfield( L, -2, "valid" );
                            CoronaLuaDispatchEvent( L, listener, 0 );
                        }
                    }
                } else {
                    if(listener) {
                        CoronaLuaNewEvent( L, "command" );
                        lua_pushstring( L, [[error description] UTF8String] );
                        lua_setfield( L, -2, "error" );
                        CoronaLuaDispatchEvent( L, listener, 0 );
                    }
                }
            }];
        } else if([command_string isEqualToString: @"applyReferralCode"]) {
            [branch applyReferralCode:[params objectForKey:@"code"] andCallback:^(NSDictionary *params, NSError *error) {
                if (!error) {
                    if(listener) {
                        CoronaLuaNewEvent( L, "command" );
                        lua_pushboolean( L, YES );
                        lua_setfield( L, -2, "valid" );
                        CoronaLuaDispatchEvent( L, listener, 0 );
                    }
                } else {
                    if(listener) {
                        CoronaLuaNewEvent( L, "command" );
                        lua_pushstring( L, [[error description] UTF8String] );
                        lua_setfield( L, -2, "error" );
                        CoronaLuaDispatchEvent( L, listener, 0 );
                    }
                }
            }];
        }
        
        
    }
    
    // No options table passed in
    else
    {
        luaL_error( L, "Error: call(), options table expected, got %s", luaL_typename( L, -1 ) );
    }
    

	return 0;
}

CORONA_EXPORT int luaopen_plugin_branch( lua_State *L )
{
	return PluginBranch::Open( L );
}
