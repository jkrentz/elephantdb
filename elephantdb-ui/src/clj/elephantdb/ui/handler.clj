(ns elephantdb.ui.handler
  (:use compojure.core
        hiccup.bootstrap.middleware
        hiccup.core
        hiccup.page
        hiccup.def
        hiccup.element
        hiccup.bootstrap.page
        hiccup.bootstrap.element
        elephantdb.ui.thrift
        ring.adapter.jetty)
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [elephantdb.client :as c]
            [carica.core :refer [configurer
                                 resources]])
  (:gen-class))

(def VERSION "0.4.4-SNAPSHOT")

(def config (configurer (resources "ui_config.clj")))

(def hosts (config :hosts))

(defn nodes [host]
  (c/with-elephant host 3578 c
    (cond (c/fully-loaded? c) [:span {:class "label label-success"} "Ready"]
          (c/updating? c) [:span {:class "label label-info"} "Updating"]
          :else [:span {:class "label label-error"} "Error"])))

(defn domains [host]
  (c/with-elephant host 3578 c
    (when-let [statuses (c/get-status c)]
      (for [[domain status] (.get_domain_statuses statuses)]
        [(link-to (str "/node/" host "/domain/" domain) domain) (domain-status->elem status)]))))

(defn template [title & body]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width initial-scale=1.0"}]
    [:title title]
    (include-bootstrap)]
   [:body
    [:div.page-header
     [:h1 "ElephantDB " [:small VERSION]]
     ]
    [:div.container {:id "content"}
     body]]))

(defn index []
  (template "An ElephantDB Never Forgets"
            [:ul.breadcrumb
             [:li.active "Cluster"]]
            [:div
             (table
              :styles [:condensed]
              :head ["Hostname" "Status"]
              :body (for [h hosts]
                      [(link-to (str "/node/" h) h) (nodes h)]))
             ]))

(defn node [id]
  (template (str "ElephantDB | " id)
            [:ul.breadcrumb
             [:li (link-to "/" "Cluster") [:span.divider "/"]]
             [:li.active id]]
            [:div
             (table
              :styles [:condensed]
              :head ["Domain" "Status"]
              :body (domains id))]))

(defroutes app-routes
  (GET "/" [] (index))
  (GET "/node/:id" [id] (node id))
  (GET "/node/:id/domain/:domain" [id domain] (str "Detail for " domain " on " id))
  (route/resources "/")
  (route/not-found "Not Found"))

(def app
  (wrap-bootstrap-resources (handler/site app-routes)))

(defn start-server [port]
  (run-jetty app {:port port :join? false}))

(defn -main []
  (prn (config :oh-hai))
  (start-server (config :listen-port)))
