(ns martian.re-frame-test
  (:require [martian.re-frame :as martian]
            [martian.core :refer [url-for]]
            [cljs.test :refer-macros [deftest testing is run-tests async]]
            [cljs.core.async :refer [<! timeout]]
            [re-frame.core :as re-frame]
            [re-frame.db :as rdb]
            [re-frame.subs :as subs]
            [day8.re-frame.test :as rf-test])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def interceptors [re-frame/trim-v])

(re-frame/reg-event-db
 ::create-pet-success
 interceptors
 (fn [db [{:keys [body]} operation-id params]]
   (assoc db :pet-id (:id body))))

(re-frame/reg-event-db
 ::http-failure
 interceptors
 (fn [db [response operation-id params]]
   (update db :errors conj operation-id)))

(deftest re-frame-test
  (rf-test/run-test-async

   (martian/init "http://localhost:8888/swagger.json")

   (rf-test/wait-for
    [::martian/init]

    (testing "can subscribe to the instance"
      (let [m @(re-frame/subscribe [::martian/instance])]
        (is (= "http://localhost:8888/pets/123"
               (url-for m :get-pet {:id 123})))))

    (testing "can make http requests by dispatching an event"
      (re-frame/dispatch [:http/request
                          :create-pet
                          {:name "Doggy McDogFace"
                           :type "Dog"
                           :age 3}
                          ::create-pet-success
                          ::http-failure]))

    (rf-test/wait-for
     [#{::create-pet-success ::http-failure}]
     (is (= 123 (:pet-id @rdb/app-db)))))))
