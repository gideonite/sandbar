;; Copyright (c) Brenton Ashworth. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns sandbar.stateful-session-test
  (:require ring.middleware.flash)
  (:use (clojure test)
        (ring.middleware.session store)
        (sandbar stateful-session
                 [test :only (t)])))

(def session-key :sandbar.stateful-session/session)

(deftest wrap-stateful-session*-test
  (t "stateful session"
     (t "input empty, session in response"
        (is (= ((wrap-stateful-session* (fn [r] {:session {:a "a"}})) {})
               {:session {:a "a"}})))
     (t "input empty, use session-put!"
        (is (= ((wrap-stateful-session* (fn [r] (do (session-put! :a "a")
                                                   {}))) {})
               {:session {session-key {:a "a"}}})))
     (t "input contains values, use session-put! and return empty session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "a")
                             {})))
                {:session {:b "b"}})
               {:session {session-key {:a "a"} :b "b"}})))
     (t "input contains values, use session-put! and return empty session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "a")
                             {:session {}})))
                {:session {:b "b"}})
               {:session {session-key {:a "a"}}})))
     (t "input contains values, use session-put! and return empty session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "a")
                             {:session nil})))
                {:session {:b "b"}})
               {:session {session-key {:a "a"}}})))
     (t "input contains values, use sesion-put! and return nil session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "a")
                             {:session nil})))
                {:session {:b "b"}})
               {:session {session-key {:a "a"}}})))
     (t "input contains values, session in response replaces values"
        (is (= ((wrap-stateful-session*
                 (fn [r] {:session {:a "a"}}))
                {:session {:b "b"}})
               {:session {:a "a"}})))
     (t "input empty, use session-put! and return session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "a")
                             {:session {:b "b"}})))
                {})
               {:session {session-key {:a "a"} :b "b"}})))
     (t "input contains values, use session-put! and return session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "a")
                             {:session {:b "b"}})))
                {:session {:c "c"}})
               {:session {session-key {:a "a"} :b "b"}})))
     (t "input contains values, return empty DOES NOT remove values"
        (is (= ((wrap-stateful-session* (fn [r] {}))
                {:session {:a "a"}})
               {:session {:a "a"}})))
     (t "input contains values, return session nil removes values"
        (is (= ((wrap-stateful-session* (fn [r] {:session nil}))
                {:session {:a "a"}})
               {:session nil})))
     (t "input contains values, return session nil removes only func values"
        (is (= ((wrap-stateful-session* (fn [r] {:session nil}))
                {:session {:a "a" session-key {:a "a"}}})
               {:session {session-key {:a "a"}}})))
     (t "session-delete-key! does not remove values from functional session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-delete-key! :a)
                             {})))
                {:session {:a "a"}})
               {:session {:a "a"}})))
     (t "session-delete-key! does remove values from sandbar-session"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-delete-key! :a)
                             {})))
                {:session {session-key {:a "a"} :a "a"}})
               {:session {:a "a"}})))
     (t "session-delete-key! causes session to be deleted when it is empty"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-delete-key! :a)
                             {})))
                {:session {session-key {:a "a"}}})
               {:session nil})))
     (t "session-delete-key! causes session to be deleted when it is empty, when combined with wrap-flash"
        (is (= ((wrap-stateful-session*
                 (ring.middleware.flash/wrap-flash
                  (fn [r] (do (session-delete-key! :a)
                              {}))))
                {:session {session-key {:a "a"}}})
               {:session nil})))
     (t "destroy-session! works"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (destroy-session!)
                             {})))
                {:session {session-key {:a "a" :b "b"}}})
               {:session nil})))
     (t "destroy-session! works when combined with wrap-flash"
        (is (= ((wrap-stateful-session*
                 (ring.middleware.flash/wrap-flash
                  (fn [r] (do (destroy-session!)
                              {}))))
                {:session {session-key {:a "a" :b "b"}}})
               {:session nil})))
     (t "response values DO NOT override existing session values"
        (is (= ((wrap-stateful-session*
                 (fn [r] (do (session-put! :a "b")
                             {:session {:a "c"}})))
                {:session {:a "a"}})
               {:session {session-key {:a "b"} :a "c"}})))
     (t "no session in request or response or sandbar-session"
        (is (= ((wrap-stateful-session*
                 (fn [r] {}))
                {})
               {})))
     (t "sandbar-sesssion should return nil when the handler returns nil"
        (is (= ((wrap-stateful-session*
                 (fn [r] nil)) {:session {}})
               nil)))))

(deftype TestSessionStore [session-map]
  SessionStore
  (read-session [_ _]
    (@session-map :s {}))
  (write-session [_ _ data]
    (swap! session-map assoc :s data))
  (delete-session [_ _]
    (swap! session-map dissoc :s)))

(defn test-session-store []
  (TestSessionStore. (atom {})))

(deftest warp-stateful-session-test
  (let [store (test-session-store)
        handler (fn [r] (do (session-put! :x 1)
                            {:status 200 :body "hello"}))
        handler (wrap-stateful-session handler {:store store})
        result (handler {})]
    (is (= (:body result) "hello"))
    (is (= (-> (read-session store nil) session-key :x) 1))))

(deftest update-session!-test
  (binding [sandbar-session (atom {})]
    (is (= (:t (update-session! (fn [a b] (assoc a :t b)) "t"))
           "t"))))

(deftest session-put!-test
  (binding [sandbar-session (atom {})]
    (let [r (session-put! :t "t")]
      (is (= (session-get :t) "t")))))

(deftest session-get-test
  (binding [sandbar-session (atom {})]
    (let [r (session-put! :t "t")]
      (is (= (session-get :t) "t")))))

(deftest flash-put!-test
  (binding [sandbar-flash (atom {})]
    (let [r (flash-put! :t "t")]
      (is (= (flash-get :t) "t")))))

(deftest flash-get-test
  (binding [sandbar-flash (atom {})]
    (let [r (flash-put! :t "t")]
      (is (= (flash-get :t) "t")))))




