(ns re-complete.utils
  (:require [clojure.string :as string]))

(defn items-to-autocomplete
  "List of the items to autocomplete by given input and list of the all items"
  [items input]
  (let [new-items
        (if (= (re-find #"[aA-zZ]" (str (first input))) nil)
          []
          (if (= (string/upper-case (first input)) (first input))
            (map string/capitalize items)
            (map string/lower-case items)))]
    (filter #(string/starts-with? % input) new-items)))

(defn regex-char-esc-smap
  "Escapes characters in string by \\ and |"
  [string]
  (let [esc-chars string]
    (zipmap esc-chars
            (map #(str "\\" % "|") esc-chars))))

(defn str-to-pattern
  "Functions transform given string to pattern"
  [string]
  (->> string
       (replace (regex-char-esc-smap string))
       (reduce str)
       re-pattern))


(defn partition-by-regexp
  "Function partitions characters from string into multiple strings by given regexp"
  [word item-regex]
  (map #(string/join "" %)
       (partition-by #(re-find (str-to-pattern item-regex) %) (mapv str word))))


(defn autocomplete-options
  "Autocomplete options for word"
  [input items options]
  (let [last-string (last (string/split input #" "))
        item-regex (:new-item-regex options)
        sort-fn (:sort-fn options)
        autocomplete-items (items-to-autocomplete items last-string)]
    (if item-regex
      (if (= (first last-string) (re-find (str-to-pattern item-regex) (str (first last-string))))
        (->> 1
             (subs last-string)
             (items-to-autocomplete items)
             (sort-by sort-fn))
        (sort-by sort-fn autocomplete-items))
      (sort-fn autocomplete-items))))

(defn autocomplete-regex-word
  "Autocomplete word and ignore regex at the beginning and at the end of the word"
  [index-in-word word word-to-autocomplete regex-item]
  (let [partitioned-by-regex (vec (partition-by-regexp word regex-item))
        index-of-part-to-autocomplete (-> index-in-word
                                          inc
                                          (take word)
                                          (partition-by-regexp regex-item)
                                          count
                                          dec)]
    (->> (update-in partitioned-by-regex [index-of-part-to-autocomplete]
                    #(str word-to-autocomplete))
         (string/join ""))))

(defn autocomplete-word-to-string
  "Autocomplete regex-word to input string"
  [index regex-item text word-to-autocomplete]
  (let [text-to-index (-> index
                          inc
                          (take text))
        words-to-index (string/split (string/join "" text-to-index) #" ")
        index-of-word (-> words-to-index
                          count
                          dec)
        position-index-in-word (if (= (count words-to-index) 1)
                                 index
                                 (->> words-to-index
                                      drop-last
                                      (string/join "")
                                      count inc
                                      (- index)))]
    (->> (update-in (string/split text #" ") [index-of-word]
                    #(autocomplete-regex-word position-index-in-word % word-to-autocomplete regex-item))
         (string/join " "))))
