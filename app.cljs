(ns app)
(require '[reagent.core :as r]
         '[reagent.dom :as rdom]
         '[clojure.string :as str])

(def logs? true)
(def minute 60000)
(def update-interval (* 5 minute))

(def log (fn [& args] (when logs? (apply js/console.log args))))

(defn schedule-data [] [{:should "play" :may "nurse" :timeFrom "07:00" :timeTo "08:00"}
                        {:should "nurse" :may "play" :timeFrom "09:00" :timeTo "09:30"}
                        {:should "nap" :may "play" :timeFrom "09:30" :timeTo "10:00"}
                        {:should "play" :may "want to nurse" :timeFrom "10:00" :timeTo "14:00"}
                        {:should "nap" :may "want to try some real food" :timeFrom "14:00" :timeTo "15:00"}
                        {:should "nurse" :may "want to nap" :timeFrom "15:00" :timeTo "16:00"}
                        {:should "play" :may "want to nap" :timeFrom "16:00" :timeTo "17:00"}
                        {:should "nurse" :may "want to play" :timeFrom "17:00" :timeTo "18:00"}
                        {:should "get pre-sleep bath" :may "want to play with a rubber ducky" :timeFrom "18:00" :timeTo "18:30"}
                        {:should "get ready for bed" :may "just want to snuggle" :timeFrom "18:30" :timeTo "19:00"}
                        {:should "be asleep" :may "need to be soothed back to sleep" :timeFrom "19:00" :timeTo "07:00"}])

(defn random-greeting []
  (let [greetings ["Hi there!" "Hello there!" "Hey there!" "Greetings!" "Howdy!"]]
    (rand-nth greetings)))

(defn schedule-select
  "Select the event from the schedule-data based on the current time in the specified timezone (defaults to EST)"
  ([events] (schedule-select events "America/New_York"))
  ([events timezone]
   (let [now (js/Date.)
         tz-now (js/Date. (.toLocaleString now "en-US" #js {:timeZone timezone}))
         current-time (.getHours tz-now)
         current-mins (.getMinutes tz-now)
         parse-time (fn [time-str]
                      (let [[hours minutes] (map js/parseInt (str/split time-str #":"))
                            hours (if (< hours 24) hours 0)]
                        {:hours hours
                         :minutes (or minutes 0)}))]
     (->> events
          (filter (fn [{:keys [timeFrom timeTo]}]
                    (let [from-time (parse-time timeFrom)
                          to-time (parse-time timeTo)]
                      (log "Checking time range:" timeFrom "-" timeTo)  ; Debug log
                      (log "Current time in" timezone ":"
                           (.toLocaleTimeString tz-now "en-US"
                                                #js {:timeZone timezone
                                                     :hour12 false}))   ; Debug log
                      (let [is-overnight? (> (:hours from-time) (:hours to-time))
                            current-total-mins (+ (* current-time 60) current-mins)
                            from-total-mins (+ (* (:hours from-time) 60) (:minutes from-time))
                            to-total-mins (+ (* (:hours to-time) 60) (:minutes to-time))
                            adjusted-current (if (and is-overnight?
                                                      (< current-total-mins from-total-mins)
                                                      (< current-total-mins to-total-mins))
                                               (+ current-total-mins 1440) ; Add 24 hours in minutes
                                               current-total-mins)
                            adjusted-to (if is-overnight?
                                          (+ to-total-mins 1440)
                                          to-total-mins)]
                        (and (>= adjusted-current from-total-mins)
                             (<= adjusted-current adjusted-to))))))
          first))))

(def current-schedule (r/atom nil))

(defn update-schedule []
  (let [selected (schedule-select (schedule-data))]
    (when (not= @current-schedule selected)
      (reset! current-schedule selected))))

(defn schedule-display [event]
  [:div {:className "text-white"}
   [:p {:className "text-4xl"} (:timeFrom event) " - " (:timeTo event)]
   [:p {:className "text-8xl font-bold"} (random-greeting)]
   [:p {:className "text-8xl font-bold mb-4"} "Baby should " (:should event) " but may " (:may event) "."]
   [:p {:className "text-4xl"} "Schedule is not set in stone, it is just a guide."]])

(defn app []
  (r/create-class
   {:component-did-mount
    (fn [] (js/setInterval update-schedule update-interval))

    :component-will-unmount
    (fn [this]
      (when-let [interval (.-interval this)]
        (js/clearInterval interval)))

    :reagent-render
    (fn []
      [:div {:className "w-full h-[100vh] flex justify-start items-center p-8 bg-teal-600"}
       [schedule-display (or @current-schedule (schedule-select (schedule-data)))]])}))

(defn root []
  [app])

(rdom/render [root] (.getElementById js/document "app"))