--[[
//
The MIT License (MIT)

Copyright (c) 2014 Gremlin Interactive Limited

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
// ----------------------------------------------------------------------------
--]]

math.randomseed( os.time() )

local branch = require( "plugin.branch" )
branch_app_key = "106349273615958200"

local branch_loaded = function(event)
  if(event.state == "ready") then
    local response = function(event)
      if(event.error) then
        print(event.error)
      elseif(event.credits) then
        print(event.credits)
      else
        print(event.data)
      end
    end
    branch.call({command="getLatestReferringParams", response=response})

    branch.call({command="getFirstReferringParams", response=response})

    branch.call({command="setIdentity", params={id="identity"}})

    branch.call({command="userCompletedAction", params={event="event", appState={attributes="here"}}})

    local url_response = function(event)
      if(event.error) then
        print(event.error)
      elseif(event.url) then
        print(event.url)
      end
    end
    branch.call({
      command="getShortURLWithParams",
      response=url_response,
      params={params={data="string"}, andTags={"tag","tag2"}, andChannel="channel", andFeature="feature", andStage="stage", andAlias="alias298"..math.random(1,10000)}
    })

    local credit_response = function(event)
      if(event.error) then
        print(event.error)
      elseif(event.credits) then
        print(event.credits)
      end
    end

    branch.call({command="loadRewardsWithCallback", response=credit_response})

    branch.call({command="redeemRewards", params={credits=5}, response=response})

    local history_response = function(event)
      if(event.error) then
        print(event.error)
      elseif(event.history) then
        print(event.history)
      end
    end

    branch.call({command="getCreditHistoryWithCallback", response=history_response})

    local get_referral_response = function(event)
      if(event.error) then
        print(event.error)
      elseif(event.referral_code) then
        print(event.referral_code)

        local validate_apply_response = function(event)
          if(event.error) then
            print(event.error)
          elseif(event.valid) then
            print(event.valid)
          end
        end

        branch.call({command="validateReferralCode", params={code=event.referral_code, referral_code=event.referral_code}, response=validate_apply_response})

        branch.call({command="applyReferralCode", params={code=event.referral_code}, response=validate_apply_response})

      end
    end
    branch.call({command="getReferralCodeWithCallback", response=get_referral_response})

    branch.call({command="getReferralCodeWithAmount", params={amount=5}, response=get_referral_response})

    branch.call({command="getReferralCodeWithPrefix", params={prefix="BRANCHCORONA", amount=5, expiration=3600, bucket="default", calculationType=0, location=0}, response=get_referral_response})

    -- branch.call({command="logout"})
  end
end
branch.init(branch_app_key, branch_loaded)
