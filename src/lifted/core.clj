(ns lifted.core
  "Lift functions into protocols.")

;;; Varargs processing.

(defn- vararg? [arglist]
  (some #(= % '&) arglist))

(defn- fixed-args [arglist]
  (vec (take-while #(not= % '&) arglist)))

(defn- vararg [arglist]
  (last arglist))

(defn- expand-vararg [arglist]
  (let [args   (fixed-args arglist)
        vararg (vararg arglist)]
    (for [i (range (- 21 (count args)))]
      (vec (concat args
                   (for [j (range i)]
                     (symbol (str vararg "_" j))))))))

(defn- expand-varargs [arglists]
  (->> (mapcat (fn [arglist]
                 (if (vararg? arglist)
                   (expand-vararg arglist)
                   [arglist]))
               arglists)
       (distinct)))

(defn- strip-vararg [arglists]
  (->> (map (fn [arglist]
              (if (vararg? arglist)
                (fixed-args arglist)
                arglist))
            arglists)
       (distinct)))


;;; Macro helpers.

(defn ^:no-doc lift-as* [ns opts]
  (for [[fsym fvar] (ns-interns ns)
        :let        [fname (str fsym)]
        :when       (= (first fname) \-)
        :let        [fmeta     (meta fvar)
                     fstripped (symbol (subs fname 1))
                     arglists  (if (contains? (:expand-varargs-for opts) fsym)
                                 (expand-varargs (:arglists fmeta))
                                 (strip-vararg (:arglists fmeta)))]
        :when       (every? not-empty arglists)]
    `(~fstripped ~@arglists ~(:doc fmeta))))


(defn ^:no-doc lift-on* [ns protocol-sym obj-sym opts]
  (let [sigs (some->> protocol-sym (ns-resolve ns) deref :sigs vals)]
    (assert sigs "not a protocol")
    (for [{fsym :name arglists :arglists} sigs
          arglist                         arglists]
      (let [impl-ns  (some-> (get opts :impl-ns (namespace protocol-sym)) str)
            impl-sym (symbol impl-ns (str "-" fsym))
            fmeta    (meta (resolve impl-sym))]
        (if (:private fmeta)
          `(~fsym ~arglist (@(var ~impl-sym) ~obj-sym ~@(rest arglist)))
          `(~fsym ~arglist (~impl-sym        ~obj-sym ~@(rest arglist))))))))


;;; Public API

(defprotocol Lifted
  (lifted [this]
    "Returns the object that was lifted using lift-on."))

(defmacro lift-as
  "Lift sthe functions in the current namespace into a protocol. Only
  functions prefixed with the - character and taking at least one
  argument are lifted. The functions are lifted into a protocol with
  the given name, where the prefix is stripped from the protocol
  function names. Does not support destructuring in the functions'
  signatures.

  An options map can be supplied. The following options are supported:

  :expand-varargs-for #{-my-fn ...}

  Expands the vararg of a function into argument lists for up to 20
  arguments total."
  ([name]
   `(lift-as ~name nil))
  ([name opts]
   `(defprotocol ~name
      ~@(lift-as* *ns* opts))))

(defmacro lift-on
  "Create a protocol implementation for the given protocol. The protocol
  implementation calls \"lifted\" functions, receiving the given obj
  as its first parameter.

  An options map can be supplied. The following options are supported:

  :impl-ns my.impl.mock

  Instead of calling the lifted functions in the namespace of the
  protocol, it will call them in the specified namespace."
  ([protocol obj]
   `(lift-on ~protocol ~obj nil))
  ([protocol obj opts]
   (assert (resolve protocol) "unknown protocol")
   (let [obj-sym (gensym)]
     `(let [~obj-sym ~obj]
        (reify ~protocol
          ~@(lift-on* *ns* protocol obj-sym opts)
          lifted.core/Lifted
          (~'lifted [~'_] ~obj-sym))))))
