(ns lifted.core
  "Lift functions into protocols.")

;;; Internals.

(defn- expand-vararg [arglists]
  (->> (mapcat (fn [arglist]
                 (if (some #(= % '&) arglist)
                   (let [args   (take-while #(not= % '&) arglist)
                         vararg (last arglist)]
                     (for [i (range (- 21 (count args)))]
                       (vec (concat args (for [j (range i)] (symbol (str vararg "_" j)))))))
                   [arglist]))
               arglists)
       (distinct)))

(defn- strip-vararg [arglists]
  (->> (map (fn [arglist]
              (if (some #(= % '&) arglist)
                (vec (take-while #(not= % '&) arglist))
                arglist))
            arglists)
       (distinct)))


;;; Public API

(defprotocol Lifted
  (lifted [this]
    "Returns the object that was lifted using lift-on."))

(defmacro lift-as
  "Lift the functions in the current namespace, which names are prefixed
  with the - character and takes at least one argument. They are
  lifted into a protocol with the given name, where the prefix is
  stripped from the protocol function names. Do not use destructuring
  in the functions' signatures.

  An options map can be supplied. The following options are supported:

  :expand-vararg-for #{-my-fn ...}

  Expands the vararg of a function into argument lists for up to 20
  arguments total."
  ([name]
   `(lift-as ~name nil))
  ([name opts]
   `(defprotocol ~name
      ~@(for [[s v] (ns-interns *ns*)
              :let  [fname    (str s)
                     vmeta    (meta v)
                     arglists (if (contains? (:expand-vararg-for opts) s)
                                (expand-vararg (:arglists vmeta))
                                (strip-vararg (:arglists vmeta)))]
              :when (and (= (first fname) \-)
                         (every? not-empty arglists))]
          (concat [(symbol (subs fname 1))]
                  arglists
                  [(:doc vmeta)])))))

(defmacro lift-on
  "Lift the functions in the given protocol into a protocol
  implementation. The protocol implementation calls the prefixed
  functions, receiving the given obj as its first parameter."
  [protocol obj]
  (assert (resolve protocol) "unknown protocol")
  (let [objsym (gensym)
        sigs   (-> protocol resolve deref :sigs vals)]
    (assert sigs "not a protocol")
    `(let [~objsym ~obj]
       (reify ~protocol
         ~@(for [{:keys [name arglists]} sigs
                 arglist                 arglists]
             (let [fsym  (symbol (namespace protocol) (str "-" name))
                   fmeta (meta (resolve fsym))]
               (if (:private fmeta)
                 `(~name ~arglist (@(var ~fsym) ~objsym ~@(rest arglist)))
                 `(~name ~arglist (~fsym ~objsym ~@(rest arglist))))))
         lifted.core/Lifted
         ~(list 'lifted '[_] objsym)))))
