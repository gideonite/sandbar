;; Copyright (c) Brenton Ashworth. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns sandbar.test_stateful_session
  (:use (clojure test)
        (sandbar stateful-session
                 [test :only (t)])))

(deftest test-session-put!
  (t "put in session"
     (t "one element"
        (binding [*session* (atom {})]
          (is (= (-> (session-put! :t "t")
                     :t)
                 "t"))))))


