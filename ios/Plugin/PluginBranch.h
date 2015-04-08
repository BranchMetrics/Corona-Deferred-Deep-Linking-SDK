//
//  PluginLibrary.h
//  TemplateApp
//
//  Copyright (c) 2012 __MyCompanyName__. All rights reserved.
//

#ifndef _PluginBranch_H__
#define _PluginBranch_H__

#include "CoronaLua.h"
#include "CoronaMacros.h"

// This corresponds to the name of the library, e.g. [Lua] require "plugin.branch"
// where the '.' is replaced with '_'
CORONA_EXPORT int luaopen_plugin_branch( lua_State *L );

#endif // _PluginBranch_H__
