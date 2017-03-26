import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.io.*;
public class MapReduce {

    public static void main(String[] args) {

        // the problem:

        // from here (INPUT)

        // "file1.txt" => "foo foo bar cat dog dog"
        // "file2.txt" => "foo house cat cat dog"
        // "file3.txt" => "foo foo foo bird"

        // we want to go to here (OUTPUT)

        // "foo" => { "file1.txt" => 2, "file3.txt" => 3, "file2.txt" => 1 }
        // "bar" => { "file1.txt" => 1 }
        // "cat" => { "file2.txt" => 2, "file1.txt" => 1 }
        // "dog" => { "file2.txt" => 1, "file1.txt" => 2 }
        // "house" => { "file2.txt" => 1 }
        // "bird" => { "file3.txt" => 1 }

        // in plain English we want to

        // Given a set of files with contents
        // we want to index them by word
        // so I can return all files that contain a given word
        // together with the number of occurrences of that word
        // without any sorting

        ////////////
        // INPUT:
        ///////////

        String currdir = System.getProperty("user.dir");
        List<String> fileNames =new ArrayList<>();
        Map<String,String>inputs = new HashMap<String,String>();
        for (String arg : args) {
            fileNames.add(arg);
        }

        String separator = "\n";
        for (String file : fileNames) {
            String loc = currdir+file;
            String line=null;
            String fileContent = "";
            try {
                FileReader fr = new FileReader(loc);
                BufferedReader br=new BufferedReader(fr);

                while((line=br.readLine())!=null){
                    fileContent=fileContent+line+separator;
                }
                inputs.put(file,fileContent);

                br.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



//        Map<String, String> input = new HashMap<String, String>();
//        input.put("file1.txt", "foo foo bar cat dog dog");
//        input.put("file2.txt", "foo house cat cat dog");
//        input.put("file3.txt", "foo foo foo bird");
//
//        // APPROACH #1: Brute force
//        {
//            Map<String, Map<String, Integer>> output = new HashMap<String, Map<String, Integer>>();
//
//            Iterator<Map.Entry<String, String>> inputIter = input.entrySet().iterator();
//            while(inputIter.hasNext()) {
//                Map.Entry<String, String> entry = inputIter.next();
//                String file = entry.getKey();
//                String contents = entry.getValue();
//
//                String[] words = contents.trim().split("\\s+");
//
//                for(String word : words) {
//
//                    Map<String, Integer> files = output.get(word);
//                    if (files == null) {
//                        files = new HashMap<String, Integer>();
//                        output.put(word, files);
//                    }
//
//                    Integer occurrences = files.remove(file);
//                    if (occurrences == null) {
//                        files.put(file, 1);
//                    } else {
//                        files.put(file, occurrences.intValue() + 1);
//                    }
//                }
//            }
//
//            // show me:
//            System.out.println(output);
//        }
//
//
//        // APPROACH #2: MapReduce
//        {
//            Map<String, Map<String, Integer>> output = new HashMap<String, Map<String, Integer>>();
//
//            // MAP:
//
//            List<MappedItem> mappedItems = new LinkedList<MappedItem>();
//
//            Iterator<Map.Entry<String, String>> inputIter = input.entrySet().iterator();
//            while(inputIter.hasNext()) {
//                Map.Entry<String, String> entry = inputIter.next();
//                String file = entry.getKey();
//                String contents = entry.getValue();
//
//                map(file, contents, mappedItems);
//            }
//
//            // GROUP:
//
//            Map<String, List<String>> groupedItems = new HashMap<String, List<String>>();
//
//            Iterator<MappedItem> mappedIter = mappedItems.iterator();
//            while(mappedIter.hasNext()) {
//                MappedItem item = mappedIter.next();
//                String word = item.getWord();
//                String file = item.getFile();
//                List<String> list = groupedItems.get(word);
//                if (list == null) {
//                    list = new LinkedList<String>();
//                    groupedItems.put(word, list);
//                }
//
//                list.add(file);
//
//            }
//
//            // REDUCE:
//
//            Iterator<Map.Entry<String, List<String>>> groupedIter = groupedItems.entrySet().iterator();
//            while(groupedIter.hasNext()) {
//                Map.Entry<String, List<String>> entry = groupedIter.next();
//                String word = entry.getKey();
//                List<String> list = entry.getValue();
//
//                reduce(word, list, output);
//            }
//
//            System.out.println(output);
//        }


        // APPROACH #3: Distributed MapReduce
        {
            final Map<String, Map<String, Integer>> output = new HashMap<String, Map<String, Integer>>();

            // MAP:

            final List<MappedItem> mappedItems = new LinkedList<MappedItem>();

            final MapCallback<String, MappedItem> mapCallback = new MapCallback<String, MappedItem>() {
                @Override
                public synchronized void mapDone(String file, List<MappedItem> results) {
                    mappedItems.addAll(results);
                }
            };

            List<Thread> mapCluster = new ArrayList<Thread>(inputs.size());

            Iterator<Map.Entry<String, String>> inputIter = inputs.entrySet().iterator();
            while(inputIter.hasNext()) {
                Map.Entry<String, String> entry = inputIter.next();
                final String file = entry.getKey();
                final String contents = entry.getValue();

                Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        map(file, contents, mapCallback);
                    }
                });
                mapCluster.add(t);
                t.start();
            }

            // wait for mapping phase to be over:
            for(Thread t : mapCluster) {
                try {
                    t.join();
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            // GROUP:

//            =================================
//            DES' Code
//            ==================================

//            Map<String, List<String>> groupedItems = new HashMap<String, List<String>>();
//

//
//            Iterator<MappedItem> mappedIter = mappedItems.iterator();
//            while(mappedIter.hasNext()) {
//                MappedItem item = mappedIter.next();
//                String word = item.getWord();
//                String file = item.getFile();
//                List<String> list = groupedItems.get(word);
//                if (list == null) {
//                    list = new LinkedList<String>();
//                    groupedItems.put(word, list);
//                }
//                list.add(file);
//            }

            Map<Character,Map<String,List<String>>> gItems= new HashMap<>();
//            =================================
//            OUR' Code
//            ==================================
            for (MappedItem mappedItem : mappedItems) {

                String word =mappedItem.getWord();
                String file = mappedItem.getFile();

                char startletter= word.charAt(0);

                Map<String,List<String>>mapp=gItems.get(startletter);
                if(mapp==null){
                    mapp=new HashMap<>();
                    gItems.put(startletter,mapp);
                }

                List<String> listofwords = mapp.get(file);
                if(listofwords==null){
                    listofwords=new LinkedList<>();
                }
                listofwords.add(word);
                mapp.put(file,listofwords);


            }

            // REDUCE:

//            =================================
//            DES' Code
//            ==================================
//            final ReduceCallback<String, String, Integer> reduceCallback = new ReduceCallback<String, String, Integer>() {
//                @Override
//                public synchronized void reduceDone(String k, Map<String, Integer> v) {
//                    output.put(k, v);
//                }
//            };

//            =================================
//            OUR' Code
//            ==================================
            final Map<Character, Map<String, Integer>> testoutput = new HashMap<>();
            final ReduceCallback<Character,String,Integer> rCallback = new ReduceCallback<Character, String, Integer>() {
                @Override
                public synchronized void reduceDone(Character character, Map<String, Integer> results) {
                    testoutput.put(character,results);
                }
            };

//            =================================
//            DES' Code
//            ==================================
//            List<Thread> reduceCluster = new ArrayList<Thread>(groupedItems.size());
//
//            Iterator<Map.Entry<String, List<String>>> groupedIter = groupedItems.entrySet().iterator();
//            while(groupedIter.hasNext()) {
//                Map.Entry<String, List<String>> entry = groupedIter.next();
//                final String word = entry.getKey();
//                final List<String> list = entry.getValue();
//
//                Thread t = new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        reduce(word, list, reduceCallback);
//                    }
//                });
//                reduceCluster.add(t);
//                t.start();
//            }

//            =================================
//            OUR' Code
//            ==================================
            List<Thread> reduceCluster = new ArrayList<Thread>(gItems.size());
            Iterator<Map.Entry<Character,Map<String,List<String>>>> gIter = gItems.entrySet().iterator();
            while(gIter.hasNext()) {
                Map.Entry<Character, Map<String, List<String>>> entry = gIter.next();
                final char letter = entry.getKey();
                final Map<String, List<String>> mapp = entry.getValue();

                Thread t = new Thread(new Runnable(){
                    @Override
                    public void run(){
                        reduce(letter,mapp,rCallback);
                    }
                });

                reduceCluster.add(t);
                t.start();
            }
            // wait for reducing phase to be over:
            for(Thread t : reduceCluster) {
                try {
                    t.join();
                } catch(InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

//            System.out.println(output);
            System.out.println();
            System.out.println("Num. Occurences of Letters in Each file:");
            testoutput.forEach((key,value)->{
                System.out.println(key+"-->"+value);
            });
        }
    }

    public static void map(String file, String contents, List<MappedItem> mappedItems) {
        String[] words = contents.trim().split("\\s+");
        for(String word: words) {
            mappedItems.add(new MappedItem(word, file));
        }
    }

    public static void reduce(String word, List<String> list, Map<String, Map<String, Integer>> output) {
        Map<String, Integer> reducedList = new HashMap<String, Integer>();
        for(String file: list) {
            Integer occurrences = reducedList.get(file);
            if (occurrences == null) {
                reducedList.put(file, 1);
            } else {
                reducedList.put(file, occurrences.intValue() + 1);
            }
        }
        output.put(word, reducedList);
    }

    public static interface MapCallback<E, V> {

        public void mapDone(E key, List<V> values);
    }

    public static void map(String file, String contents, MapCallback<String, MappedItem> callback) {
        String[] words = contents.trim().split("\\s+");
        List<MappedItem> results = new ArrayList<MappedItem>(words.length);
        for(String word: words) {
            results.add(new MappedItem(word, file));
        }
        callback.mapDone(file, results);
    }


    public static interface ReduceCallback<E, K, V> {

        public void reduceDone(E e, Map<K,V> results);
    }

    public static void reduce(String word, List<String> list, ReduceCallback<String, String, Integer> callback) {

        Map<String, Integer> reducedList = new HashMap<String, Integer>();
        for(String file: list) {
            Integer occurrences = reducedList.get(file);
            if (occurrences == null) {
                reducedList.put(file, 1);
            } else {
                reducedList.put(file, occurrences.intValue() + 1);
            }
        }
        callback.reduceDone(word, reducedList);


    }

    public static void reduce(char letter, Map<String,List<String>> filewordmap, ReduceCallback<Character,String,Integer>callback){

        Map<String, Integer> reducedList = new HashMap<>();

        for (String file : filewordmap.keySet()) {

            int numwordsinfile=filewordmap.get(file).size();

            reducedList.put(file,numwordsinfile);
        }
        callback.reduceDone(letter,reducedList);

    }

    private static class MappedItem {

        private final String word;
        private final String file;

        public MappedItem(String word, String file) {
            this.word = word;
            this.file = file;
        }

        public String getWord() {
            return word;
        }

        public String getFile() {
            return file;
        }

        @Override
        public String toString() {
            return "[\"" + word + "\",\"" + file + "\"]";
        }
    }
}