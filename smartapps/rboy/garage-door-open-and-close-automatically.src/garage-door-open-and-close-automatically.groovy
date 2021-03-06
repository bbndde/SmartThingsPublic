/*
 * -----------------------
 * ------ SMART APP ------
 * -----------------------
 *
 * STOP:  Do NOT PUBLISH the code to GitHub, it is a VIOLATION of the license terms.
 * You are NOT allowed share, distribute, reuse or publicly host (e.g. GITHUB) the code. Refer to the license details on our website.
 *
 */

/* **DISCLAIMER**
* THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
* HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
* Without limitation of the foregoing, Contributors/Regents expressly does not warrant that:
* 1. the software will meet your requirements or expectations;
* 2. the software or the software content will be free of bugs, errors, viruses or other defects;
* 3. any results, output, or data provided through or generated by the software will be accurate, up-to-date, complete or reliable;
* 4. the software will be compatible with third party software;
* 5. any errors in the software will be corrected.
* The user assumes all responsibility for selecting the software and for the results obtained from the use of the software. The user shall bear the entire risk as to the quality and the performance of the software.
*/ 

def clientVersion() {
    return "02.02.00"
}

/*
* Garage Door Open and Close
*
* Copyright RBoy Apps, redistribution or reuse of code is not allowed without permission
*
* Change Log
* 2019-12-14 - (v02.02.00) Extend door closure when motion is detected
* 2019-10-11 - (v02.01.03) Add support for the new Sonos integration (auto detect)
* 2019-06-19 - (v02.01.02) Reset timer when motion is detected while closing garage door
* 2019-05-21 - (v02.01.01) Check for invalid SMS characters, day of week required to define operating schedule
* 2019-03-29 - (v02.01.00) Added support for checking motion for timed closures
* 2019-01-09 - (v02.00.06) Send closed notification if garage door was closed before the timer expired
* 2018-12-27 - (v02.00.05) Added icons
* 2018-12-21 - (v02.00.04) Added option for detailed notifications to reduce number of messages
* 2018-06-28 - (v02.00.02) Reset close time if door is closed and reopened and don't reset timer for delayed opening if multiple people arrive
* 2018-05-14 - (v02.00.01) Version update reinitialize fix
* 2018-04-20 - (v02.00.00) Updated to match new ST door control specifications, added timed closing options and revamped UI
* 2018-03-21 - (v01.08.03) Don't print message about turning on switches if no switches are selected
* 2018-02-13 - (v01.08.02) Added support for garage door control devices instead of door control devices
* 2017-09-06 - (v01.08.00) Added support for expirations past midnight
* 2017-07-24 - (v01.07.00) Added support for opening and closing mode selection
* 2017-05-26 - (v01.06.02) Multiple SMS numbers are now separate by a *
* 2017-04-29 - (v01.06.01) Patch for delayed opening of garage doors
* 2017-04-22 - (v01.06.00) Added support for delayed opening of garage doors
* 2016-11-05 - Added support for automatic code update notifications and fixed an issue with sms
* 2016-10-07 - Added support for Operating Schedule for arrival and departure
* 2016-08-17 - Added workaround for ST contact address book bug
* 2016-08-13 - Added support for sending SMS to multiple numbers by separating them with a +
* 2016-08-13 - Added support for contact address book from ST
* 2016-08-13 - Added support to turn on lights when someone arrives with option of doing it when it's dark outside
* 2016-02-14 - Only open/close doors if required and notify accordingly
* 2016-01-16 - Description correction
* 2016-01-16 - Added option to choose different garage doors/people for Open and Close actions
* 2016-01-15 - Added option for notitifications
* 2016-01-15 - Fix for missing handler
* 2015-10-26 - Fixed incorrect display text for arriving
* 2015-02-02 - Initial release
*
*/
definition(
    name: "Garage Door Open and Close Automatically",
    namespace: "rboy",
    author: "RBoy Apps",
    description: "Open/close a garage door when someone arrives/leaves or after a specified time and execute actions",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/garage_contact@2x.png")

preferences {
    page(name: "mainPage")
    page(name: "arrivalPage")
    page(name: "leavePage")
    page(name: "schedulePage")
    page(name: "closePage")
}

def mainPage() {
    dynamicPage(name: "mainPage", title: "Garage Door Open and Close Automatically v${clientVersion()}", install: true, uninstall: true) {
        section("Opening Garage Door(s)") {
            href(name: "arrival", title: "Open when people arrive", page: "arrivalPage", description: (arrives && doorsOpen) ? "Configured" : "Not configured", required: false, image: "http://www.rboyapps.com/images/GarageOpen.png")
        }
        
        section("Closing Garage Door(s)") {
            href(name: "leave", title: "Close when people leave", page: "leavePage", description: (leaves && doorsClose) ? "Configured" : "Not configured", required: false, image: "http://www.rboyapps.com/images/GarageClosed.png")
            href(name: "close", title: "Close when left open", page: "closePage", description: (doorsCloseTimed && closeTimed) ? "Configured" : "Not configured", required: false, image: "http://www.rboyapps.com/images/GarageClosedTimer.png")
        }
        section("Notifications") {
            input("recipients", "contact", title: "Send notifications to", multiple: true, required: false, image: "http://www.rboyapps.com/images/Notifications.png") {
                paragraph "You can enter multiple phone numbers by separating them with a '*'\nE.g. 5551234567*+448747654321"
                input "sms", "phone", title: "Send SMS notification to", required: false, image: "http://www.rboyapps.com/images/Notifications.png"
                input "push", "bool", title: "Send push notifications", defaultValue: true, required: false, image: "http://www.rboyapps.com/images/PushNotification.png"
            }
            input "audioDevices", "capability.audioNotification", title: "Speak notifications on", required: false, multiple: true, submitOnChange: true, image: "http://www.rboyapps.com/images/Horn.png"
            if (audioDevices) {
                input "audioVolume", "number", title: "...at this volume level (optional)", description: "keep current", required: false, range: "1..100"
            }
            input "detailedNotifications", "bool", title: "Send detailed notifications", defaultValue: false, required: false
        }
        section() {
            label title: "Assign a name for this SmartApp (optional)", required: false
            input "updateNotifications", "bool", title: "Check for new versions of the app", defaultValue: true, required: false
        }
    }
}

def arrivalPage() {
    dynamicPage(name: "arrivalPage", title: "Open Garage Doors When People Arrive", install: false, uninstall: false) {    
        section() {
            input "arrives", "capability.presenceSensor", title: "When one of these arrive", description: "Which people arrive?", multiple: true, required: false
            input "doorsOpen", "capability.doorControl", title: "Open these garage door(s)?", required: false, multiple: true
            input "doorsOpenDelay", "number", title: "...after these seconds", required: false
            input "arriveSwitches", "capability.switch", title: "...and turn on these switches", description: "Turn on lights", multiple: true, required: false, submitOnChange: true
            if (arriveSwitches) {
                input "arriveAfterDark", "bool", title: "...only if it's getting dark outside", description: "Turn on lights at night", required: false
            }
            input "openModes", "mode", title: "...only when in this mode(s)", required: false, multiple: true
            
            def hrefParams = [user: "A", schedule: 0 as String, passed: true] // use as String otherwise it won't work on Android
            href name: "arrivalSchedule", params: hrefParams, title: "...only during this schedule", page: "schedulePage", description: scheduleDesc(hrefParams.user, hrefParams.schedule), required: false
        }
    }
}

def leavePage() {
    dynamicPage(name: "leavePage", title: "Close Garage Doors When People Leave", install: false, uninstall: false) {    
        section() {
            input "leaves", "capability.presenceSensor", title: "When one of these leave", description: "Which people leave?", multiple: true, required: false
            input "doorsClose", "capability.doorControl", title: "Close these garage door(s)?", required: false, multiple: true
            input "leaveXPeople", "capability.motionSensor", title: "...when no motion is detected", required: false, multiple: true
            input "closeModes", "mode", title: "...only when in this mode(s)", required: false, multiple: true

            def hrefParams = [user: "B", schedule: 0 as String, passed: true] // use as String otherwise it won't work on Android
            href name: "leaveSchedule", params: hrefParams, title: "...only during this schedule", page: "schedulePage", description: scheduleDesc(hrefParams.user, hrefParams.schedule), required: false 
        }
    }
}

private scheduleDesc(schedule, i) {
    TimeZone timeZone = location.timeZone
    if (!timeZone) {
        timeZone = TimeZone.getDefault()
        def msg = "Hub geolocation not set, using ${timeZone.getDisplayName()} timezone. Use the SmartThings app to set the Hub geolocation to identify the correct timezone."
        log.error msg
        sendPush msg
        section("INVALID HUB LOCATION") {
            paragraph title: msg, required: true, ""
        }
    }

    return (settings."userDayOfWeek${schedule}${i}" ? "${settings."userDayOfWeek${schedule}${i}"}: ${settings."userStartTime${schedule}${i}" ? (new Date(timeToday(settings."userStartTime${schedule}${i}", timeZone).time)).format("HH:mm z", timeZone) : ""} - ${settings."userEndTime${schedule}${i}" ? (new Date(timeToday(settings."userEndTime${schedule}${i}", timeZone).time)).format("HH:mm z", timeZone) : ""}" : "Not defined")
}

def schedulePage(params) {
    //  params is broken, after doing a submitOnChange on this page, params is lost. So as a work around when this page is called with params save it to state and if the page is called with no params we know it's the bug and use the last state instead
    if (params.passed) {
        atomicState.params = params // We got something, so save it otherwise it's a page refresh for submitOnChange
    }

    def user = ""
    def schedule = ""
    // Get user from the passed in params when the page is loading, else get from the last saved to work around not having params on pages
    if (params.user) {
        user = params.user ?: ""
        log.trace "Passed from main page, using params lookup for user $user"
    } else if (atomicState.params) {
        user = atomicState.params.user ?: ""
        log.trace "Passed from submitOnChange, atomicState lookup for user $user"
    } else {
        log.error "Invalid params, no user found. Params: $params, saved params: $atomicState.params"
    }
    
    // Get schedule from the passed in params when the page is loading, else get from the last saved to work around not having params on pages
    if (params.schedule) {
        schedule = params.schedule ?: ""
        log.trace "Passed from main page, using params lookup for schedule $schedule"
    } else if (atomicState.params) {
        schedule = atomicState.params.schedule ?: ""
        log.trace "Passed from submitOnChange, atomicState lookup for schedule $schedule"
    } else {
        log.error "Invalid params, no schedule found. Params: $params, saved params: $atomicState.params"
    }

    log.trace "Schedule Page, schedule:$schedule, user:$user, passed params: $params, saved params:$atomicState.params"

    dynamicPage(name:"schedulePage", title: "Define operating schedule", uninstall: false, install: false) {
        def usr = user
        def i = schedule
        def priorUserDayOfWeek = settings."userDayOfWeek${usr}${i}"
        def priorUserStartTime = settings."userStartTime${usr}${i}"
        def priorUserEndTime = settings."userEndTime${usr}${i}"

        section("Operating Schedule (optional)") {
            input "userStartTime${usr}${i}", "time", title: "Start Time", required: false
            input "userEndTime${usr}${i}", "time", title: "End Time", required: false
            input "userDayOfWeek${usr}${i}",
                "enum",
                title: "Which day of the week?",
                description: "Not defined",
                required: false,
                multiple: true,
                options: [
                    'All Week',
                    'Monday to Friday',
                    'Saturday & Sunday',
                    'Monday',
                    'Tuesday',
                    'Wednesday',
                    'Thursday',
                    'Friday',
                    'Saturday',
                    'Sunday'
                ],
                defaultValue: priorUserDayOfWeek
        }
    }
}

def closePage() {
    dynamicPage(name: "closePage", title: "Close Garge Door When Left Opened", install: false, uninstall: false) {    
        section() {
            input "doorsCloseTimed", "capability.doorControl", title: "Close these garage door(s)?", required: false, multiple: true
            input "closeTimed", "number", title: "Close after (minutes)", description: "Close if left open", range: "1..*", required: false
            input "closeXPeople", "capability.motionSensor", title: "...when no motion is detected", required: false, multiple: true
            input "closeModesTimed", "mode", title: "...only when in this mode(s)", required: false, multiple: true

            def hrefParams = [user: "C", schedule: 0 as String, passed: true] // use as String otherwise it won't work on Android
            href name: "closeSchedule", params: hrefParams, title: "...only during this schedule", page: "schedulePage", description: scheduleDesc(hrefParams.user, hrefParams.schedule), required: false 
        }
    }
}

def installed() {
    log.debug "Installed with settings: ${settings}"

    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"

    initialize()
}

def initialize() {
    state.clientVersion = clientVersion() // Update our local stored client version to detect code upgrades
    
    unschedule()
    unsubscribe()

    atomicState.scheduledList = [:] // Reset any pending actions

    subscribe(arrives, "presence.present", arriveHandler)
    subscribe(leaves, "presence.not present", leaveHandler)
    subscribe(doorsCloseTimed, "door", timedDoorHandler)
    subscribe(closeXPeople, "motion", timedDoorMotionHandler)

    // Our handler runs every minute to check for pending actions
    schedule("? 0/1 * * * ?", pendingActions)

    // Check for new versions of the code
    def random = new Random()
    Integer randomHour = random.nextInt(18-10) + 10
    Integer randomDayOfWeek = random.nextInt(7-1) + 1 // 1 to 7
    schedule("0 0 " + randomHour + " ? * " + randomDayOfWeek, checkForCodeUpdate) // Check for code updates once a week at a random day and time between 10am and 6pm
}

private getScheduledList() { atomicState.scheduledList = atomicState.scheduledList ?: [:] } // Get list of pending door closures

def pendingActions() {
    log.trace "Pending actions: $scheduledList"
    
    if (versionCheck()) {
        return
    }

    scheduledList.each { dni, timestamp ->
        if (timestamp) { // If we have a valid timestamp for the device
            def current = now()
            if (current >= timestamp) {
                def data = [deviceNetworkId: dni]
                timedCloseDoors(data)
            } else {
                def door = doorsCloseTimed.find { it.deviceNetworkId == dni }
                log.trace "${door} pending closure in ${(timestamp - current)/1000/60 as Integer} minute(s)"
            }
        }
    }
}

def timedDoorMotionHandler(evt) {
    log.debug "TimedDoorMotionHandler $evt.displayName, $evt.name: $evt.value"

    if (versionCheck()) {
        return
    }
    
    def msg = ""
    if (evt.value == "active") {
        def sensor = evt.displayName
        def doors = []
        scheduledList.each { dni, timestamp -> // Cycle through pending door close actions and reset them
            if (timestamp) { // If we have a pending action
                doors << doorsCloseTimed.find { it.deviceNetworkId == dni }
                atomicState.scheduledList = (scheduledList << [(dni):(now() + (closeTimed * 60 * 1000))]) // Reset when to close door (keep only the latest open)
            }
        }
        if (doors) {
            msg += "Motion detected from $sensor, closing ${doors*.displayName.join(", ")} in $closeTimed minutes"
        }
    }
        
    if (msg) {
        log.info(msg)
        sendNotifications(msg)
    }
}

def timedDoorHandler(evt) {
    log.debug "TimedDoorHandler $evt.displayName, $evt.name: $evt.value"

    if (versionCheck()) {
        return
    }
    
    def msg = ""
    if (evt.value == "open") {
        if (closeModesTimed ? !closeModesTimed.find{it == location.mode} : false) { // Check if we are within operating modes
            log.warn "Out of operating mode, skipping closing door after timeout"
            return
        }
        
        if (settings."userDayOfWeek${"C"}${0}" && !checkSchedule(0, "C")) { // Check if we are within operating Schedule to operating things
            log.warn "Out of operating schedules, skipping closing door after timeout"
            return
        }

        if (closeTimed) {
            msg += "$evt.displayName opened, closing garage door in $closeTimed minute(s)"
            atomicState.scheduledList = (scheduledList << [(evt.device.deviceNetworkId):(now() + (closeTimed * 60 * 1000))]) // When to close door (keep only the latest open)
        }
    } else if (evt.value == "closed") { // If it's closed then reset the timer if present
        if (closeTimed) {
            log.trace "$evt.displayName closed, resetting close timer"
            atomicState.scheduledList = (scheduledList << [(evt.device.deviceNetworkId):null]) // Reset
            if (detailedNotifications) {
                msg += "$evt.displayName closed"
            }
        }
    }
        
    if (msg) {
        log.info(msg)
        sendNotifications(msg)
    }
}

def timedCloseDoors(data) {
    log.debug "Timed close garage door id: $data.deviceNetworkId"
    
    def door = doorsCloseTimed.find { it.deviceNetworkId == data.deviceNetworkId }
    log.trace "Garage door: $door.displayName"

    def msg = ""
    def resetTimer = false
    
    if (door.currentDoor != "closed") {
        def sensor = closeXPeople.find { it.currentValue("motion") == "active" }
        if (sensor) { // Check if any motion sensors are reporting active to prevent closure
            msg += "Motion detected from $sensor, closing $door in $closeTimed minutes"
            resetTimer = true
        } else {
            if (detailedNotifications) {
                msg += "$closeTimed minutes over, closing $door"
            }
            door.close()
        }
    } else {
        log.trace "$closeTimed minutes over, $door already closed"
    }
    
    // Stop tracking it
    if (resetTimer) {
        atomicState.scheduledList = (scheduledList << [(door.deviceNetworkId):(now() + (closeTimed * 60 * 1000))]) // When to close door (keep only the latest open)
    } else {
        atomicState.scheduledList = (scheduledList << [(door.deviceNetworkId):null]) // Reset it
    }

    if (msg) {
        log.info(msg)
        sendNotifications(msg)
    }
}

def delayOpenDoors(data) {
    log.debug "Delayed arriveHandler presenceId: $data.deviceNetworkId, type: $data.type"
    
    def device = arrives.find { it.deviceNetworkId == data.deviceNetworkId }
    log.trace "Presence sensor: $device.displayName"
    
    def msg = ""
    
    if (device.currentPresence != "present") {
        if (detailedNotifications) {
            msg += ", $device.displayName not present, skipping opening doors"
        }
    } else {
        for (door in doorsOpen) {
            if (door.currentDoor == "closed") {
                if (detailedNotifications) {
                    msg += ", opening $door"
                }
                door.open()
            } else {
                if (detailedNotifications) {
                    msg += ", $door already open"
                }
            }
        }
    }
    
    if (msg) { // If we have a message
        msg = "$delayOpenDoors seconds over" + msg
    }

    log.debug(msg)
    sendNotifications(msg)
}

def arriveHandler(evt) {
    log.debug "arriveHandler $evt.displayName, $evt.name: $evt.value"
    
    if (versionCheck()) {
        return
    }

    if (openModes ? !openModes.find{it == location.mode} : false) { // Check if we are within operating modes
        log.warn "Out of operating mode, skipping arrival handling"
        return
    }
    
    if (settings."userDayOfWeek${"A"}${0}" && !checkSchedule(0, "A")) { // Check if we are within operating Schedule to operating things
        log.warn "Out of operating schedules, skipping arrival handling"
        return
    }

    def msg = ""
    
    if (doorsOpenDelay) {
        if (canSchedule()) { // ST only allows 6 schedules overall
            msg += ", opening doors after $doorsOpenDelay seconds"
            runIn(doorsOpenDelay, "delayOpenDoors", [data: [deviceNetworkId: evt.device.deviceNetworkId, type: evt.value], overwrite: false]) // If multiple people arrive take the first one
        } else {
            log.error "ERROR: Unable to schedule opening doors after $doorsOpenDelay seconds, not enough schedules available"
        }
    } else {
        for (door in doorsOpen) {
            if (door.currentDoor == "closed") {
                msg += ", opening $door"
                door.open()
            } else {
                if (detailedNotifications) {
                    msg += ", $door already open"
                }
            }
        }
    }

    if (arriveAfterDark && arriveSwitches) {
        def cdt = new Date(now())
        def sunsetSunrise = getSunriseAndSunset(sunsetOffset: "-01:00") // Turn on 1 hour before sunset (dark)
        log.trace "Current DT: $cdt, Sunset $sunsetSunrise.sunset, Sunrise $sunsetSunrise.sunrise"
        if ((cdt >= sunsetSunrise.sunset) || (cdt <= sunsetSunrise.sunrise)) {
            arriveSwitches?.on() // Turn on switches after dark
            msg += ", turning on $arriveSwitches because it's getting dark outside"
        }
    } else if (arriveSwitches) {
        arriveSwitches?.on() // Turn on switches on arrival
        msg += ", turning on $arriveSwitches"
    }

    if (msg) { // If we have a message
        msg = "$evt.displayName arrived" + msg
    }

    log.debug(msg)
    sendNotifications(msg)
}

def leaveHandler(evt) {
    log.debug "leaveHandler $evt.displayName, $evt.name: $evt.value"
    
    if (versionCheck()) {
        return
    }

    if (closeModes ? !closeModes.find{it == location.mode} : false) { // Check if we are within operating modes
        log.warn "Out of operating mode, skipping departure handling"
        return
    }
    
    if (settings."userDayOfWeek${"B"}${0}" && !checkSchedule(0, "B")) { // Check if we are within operating Schedule to operating things
        log.warn "Out of operating schedules, skipping departure handling"
        return
    }

    def msg = ""
    def sensor = leaveXPeople.find { it.currentValue("motion") == "active" }
    if (sensor) { // Check if any motion sensors are reporting active to prevent closure
        msg += ", motion detected from $sensor, not closing $doorsClose"
    } else {
        for (door in doorsClose) {
            if (door.currentDoor == "open") {
                msg += ", closing $door"
                door.close()
            } else {
                if (detailedNotifications) {
                    msg += ", $door already closed"
                }
            }
        }
    }
    
    if (msg) { // If we have a message
        msg = "$evt.displayName left" + msg
    }

    log.debug(msg)
    sendNotifications(msg)
}

private versionCheck() {
    // Check if the user has upgraded the SmartApp and reinitailize if required
    if (state.clientVersion != clientVersion()) {
        def msg = "NOTE: ${app.label} detected a code upgrade. Updating configuration, please open the app and click on Save to re-validate your settings"
        log.warn msg
        runIn(1, initialize) // Reinitialize the app offline
        sendNotifications(msg) // Do this in the end as it may timeout
        return true
    }
    
    return false
}

private void sendText(number, message) {
    if (number) {
        def phones = number.replaceAll("[;,#]", "*").split("\\*") // Some users accidentally use ;,# instead of * and ST can't handle *,#+ in the number except for + at the beginning
        for (phone in phones) {
            try {
                sendSms(phone, message)
            } catch (Exception e) {
                sendPush "Invalid phone number $phone"
            }
        }
    }
}

private void sendNotifications(message) {
	if (!message) {
		return
    }
    
    if (location.contactBookEnabled) {
        sendNotificationToContacts(message, recipients)
    } else {
        if (push) {
            sendPush message
        } else {
            sendNotificationEvent(message)
        }
        if (sms) {
            sendText(sms, message)
        }
    }

    audioDevices?.each { audioDevice -> // Play audio notifications
        if (audioDevice.hasCommand("playText")) { // Check if it supports TTS
            if (audioVolume) { // Only set volume if defined as it also resumes playback
                audioDevice.playTextAndResume(message, audioVolume)
            } else {
                audioDevice.playText(message)
            }
        } else {
            if (audioVolume) { // Only set volume if defined as it also resumes playback
                audioDevice.playTrackAndResume(textToSpeech(message)?.uri, audioVolume) // No translations at this time
            } else {
                audioDevice.playTrack(textToSpeech(message)?.uri) // No translations at this time
            }
        }
    }
}

// Checks if we are within the current operating scheduled
// Inputs to the function are user (i) and schedule (x) (there can be multiple schedules)
// Preferences required in user input settings are:
// settings."userStartTime${x}${i}" - optional
// settings."userEndTime${x}${i}" - optional
// settings."userDayOfWeek${x}${i}" - required
private checkSchedule(def i, def x) {
    log.trace "Checking operating schedule $x for user $i"

    TimeZone timeZone = location.timeZone
    if (!timeZone) {
        timeZone = TimeZone.getDefault()
        def msg = "Hub geolocation not set, using ${timeZone.getDisplayName()} timezone. Use the SmartThings app to set the Hub geolocation to identify the correct timezone."
        log.error msg
        sendPush msg
    }

    def doChange = false
    Calendar localCalendar = Calendar.getInstance(timeZone)
    int currentDayOfWeek = localCalendar.get(Calendar.DAY_OF_WEEK)
    def currentDT = new Date(now())

    // some debugging in order to make sure things are working correclty
    log.trace "Current time: ${currentDT.format("EEE MMM dd yyyy HH:mm z", timeZone)}"

    // Check if we are within operating times
    if (settings."userStartTime${x}${i}" && settings."userEndTime${x}${i}") {
        def scheduledStart = timeToday(settings."userStartTime${x}${i}", timeZone)
        def scheduledEnd = timeToday(settings."userEndTime${x}${i}", timeZone)

        if (scheduledEnd <= scheduledStart) { // End time is next day
            def localHour = currentDT.getHours() + (int)(timeZone.getOffset(currentDT.getTime()) / 1000 / 60 / 60)
            //log.trace "Local hour is $localHour"
            if (( localHour >= 0) && (localHour < 12)) // If we between midnight and midday
            {
                log.trace "End time is before start time and we are past midnight, assuming start time is previous day"
                scheduledStart = scheduledStart.previous() // Get the start time for yesterday
            } else {
                log.trace "End time is before start time and we are past midday, assuming end time is the next day"
                scheduledEnd = scheduledEnd.next() // Get the end time for tomorrow
            }
        }

        log.trace "Operating Start ${scheduledStart.format("HH:mm z", timeZone)}, End ${scheduledEnd.format("HH:mm z", timeZone)}"

        if (currentDT < scheduledStart || currentDT > scheduledEnd) {
            log.debug "Outside operating time schedule"
            return false
        }
    }

    // Check the condition under which we want this to run now
    // This set allows the most flexibility.
    log.trace "Operating DOW(s): ${settings."userDayOfWeek${x}${i}"}"

    if(!settings."userDayOfWeek${x}${i}") {
        log.debug "Day of week not specified for operating schedule $x for user $i"
        return false
    } else if(settings."userDayOfWeek${x}${i}".contains('All Week')) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Monday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.MONDAY) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Tuesday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.TUESDAY) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Wednesday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.WEDNESDAY) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Thursday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.THURSDAY) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Friday') || settings."userDayOfWeek${x}${i}".contains('Monday to Friday')) && currentDayOfWeek == Calendar.instance.FRIDAY) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Saturday') || settings."userDayOfWeek${x}${i}".contains('Saturday & Sunday')) && currentDayOfWeek == Calendar.instance.SATURDAY) {
        doChange = true
    } else if((settings."userDayOfWeek${x}${i}".contains('Sunday') || settings."userDayOfWeek${x}${i}".contains('Saturday & Sunday')) && currentDayOfWeek == Calendar.instance.SUNDAY) {
        doChange = true
    }

    // If we have hit the condition to schedule this then lets do it
    if(doChange == true){
        log.debug("Within operating schedule")
        return true
    }
    else {
        log.debug("Outside operating schedule")
        return false
    }
}

def checkForCodeUpdate(evt) {
    log.trace "Getting latest version data from the RBoy Apps server"
    
    def appName = "Garage Door Open and Close Automatically when People Arrive/Leave"
    def serverUrl = "http://smartthings.rboyapps.com"
    def serverPath = "/CodeVersions.json"
    
    try {
        httpGet([
            uri: serverUrl,
            path: serverPath
        ]) { ret ->
            log.trace "Received response from RBoy Apps Server, headers=${ret.headers.'Content-Type'}, status=$ret.status"
            //ret.headers.each {
            //    log.trace "${it.name} : ${it.value}"
            //}

            if (ret.data) {
                log.trace "Response>" + ret.data
                
                // Check for app version updates
                def appVersion = ret.data?."$appName"
                if (appVersion > clientVersion()) {
                    def msg = "New version of app ${app.label} available: $appVersion, current version: ${clientVersion()}.\nPlease visit $serverUrl to get the latest version."
                    log.info msg
                    if (updateNotifications != false) { // The default true may not be registered
                        sendPush(msg)
                    }
                } else {
                    log.trace "No new app version found, latest version: $appVersion"
                }
                
                // Check device handler version updates
                def caps = [ arrives, doorsOpen, arriveSwitches, leaves, doorsClose, doorsCloseTimed ]
                caps?.each {
                    def devices = it?.findAll { it.hasAttribute("codeVersion") }
                    for (device in devices) {
                        if (device) {
                            def deviceName = device?.currentValue("dhName")
                            def deviceVersion = ret.data?."$deviceName"
                            if (deviceVersion && (deviceVersion > device?.currentValue("codeVersion"))) {
                                def msg = "New version of device handler for ${device?.displayName} available: $deviceVersion, current version: ${device?.currentValue("codeVersion")}.\nPlease visit $serverUrl to get the latest version."
                                log.info msg
                                if (updateNotifications != false) { // The default true may not be registered
                                    sendPush(msg)
                                }
                            } else {
                                log.trace "No new device version found for $deviceName, latest version: $deviceVersion, current version: ${device?.currentValue("codeVersion")}"
                            }
                        }
                    }
                }
            } else {
                log.error "No response to query"
            }
        }
    } catch (e) {
        log.error "Exception while querying latest app version: $e"
    }
}

// THIS IS THE END OF THE FILE