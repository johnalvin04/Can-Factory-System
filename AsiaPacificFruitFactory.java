package AsiaPacificFruitFactory;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class AsiaPacificFruitFactory {

    //with the current requirements, inorder to completely show the process, takes roughly 250+mins. //inorder to reduce the time, the limit of the queue has to be reduced
    public static void main(String[] args) {

        LinkedBlockingQueue<can> belt = new LinkedBlockingQueue(1);
        LinkedBlockingQueue<can> presteril = new LinkedBlockingQueue(globalvalues.sterilsize);
        LinkedBlockingQueue<can> steril = new LinkedBlockingQueue(globalvalues.sterilsize);
        LinkedBlockingQueue<can> fill = new LinkedBlockingQueue(1);
        LinkedBlockingQueue<can> preseal = new LinkedBlockingQueue(globalvalues.sealsize);
        LinkedBlockingQueue<can> seal = new LinkedBlockingQueue(globalvalues.sealsize);
        LinkedBlockingQueue<can> label = new LinkedBlockingQueue(1);
        LinkedBlockingQueue<pack> packing = new LinkedBlockingQueue(globalvalues.packstobox);
        LinkedBlockingQueue<box> boxingQ = new LinkedBlockingQueue(globalvalues.boxingsize);
        LinkedBlockingQueue<box> ldbay = new LinkedBlockingQueue(globalvalues.boxingsize);
        LinkedBlockingQueue<devbox> vanQ = new LinkedBlockingQueue(1);

        monitor m = new monitor(boxingQ);

        generator gen = new generator(belt, boxingQ, ldbay, m);
        sterilisation str = new sterilisation(steril, presteril, belt, boxingQ, ldbay, m);
        filling fl = new filling(steril, fill, boxingQ, m);
        sealing sl = new sealing(fill, seal, preseal, boxingQ, m);
        labelling lb = new labelling(seal, label, boxingQ, m);
        packaging p = new packaging(label, packing, boxingQ, m);
        boxing bo = new boxing(packing, boxingQ, m);
        
        //starting the threads
     
        gen.start();
        str.start();
        fl.start();
        sl.start();
        lb.start();
        p.start();
        bo.start();
        
        //van thread
        for (int i = 1; i <= 3; i++) {
            forklift f = new forklift(boxingQ, ldbay, m, i);
            f.start();
        }
        loadingbay ld = new loadingbay(2);
        
        for (int i = 1; i <= 3; i++) {
            van v = new van(ld, i, ldbay, vanQ);
            v.start();
        }
    }
}

class can {

    int canid;
    boolean run = true;
    Random rand = new Random();
    int drand = rand.nextInt(10);
    int frand = rand.nextInt(15);
    int srand = rand.nextInt(20);
    int lrand = rand.nextInt(25);

    public can(int canid) {
        this.canid = canid;
    }
}

class pack {

    LinkedBlockingQueue<can> packedcans;
    int packid;

    public pack(LinkedBlockingQueue<can> packedcans, int packid) {
        this.packedcans = packedcans;
        this.packid = packid;
    }

}

class box {

    LinkedBlockingQueue<pack> packtobox;
    int boxid;

    public box(LinkedBlockingQueue<pack> packtobox, int boxid) {
        this.packtobox = packtobox;
        this.boxid = boxid;
    }
}

class devbox {

    LinkedBlockingQueue<box> twentybox;

    public devbox(LinkedBlockingQueue<box> twentybox) {
        this.twentybox = twentybox;
    }
}

class globalvalues {

    public static final int sterilsize = 4;//4 - assignment
    public static final int sealsize = 2;//12 - assignment || 2 - presentation
    public static final int perpacksize = 3;//6 - assignment || 2 - presentation
    public static final int packstobox = 4;//27 - assignment || 2 - presentation
    public static final int boxingsize = 1;//12 - assignment || 3 - presentation
    public static final int finalbox = 3;//20 - assignment || 2 - presentation
}

class monitor {

    LinkedBlockingQueue<box> boxingQ;

    public monitor(LinkedBlockingQueue<box> boxingQ) {
        this.boxingQ = boxingQ;
    }

    synchronized public void lock() {
        if (boxingQ.size() == globalvalues.boxingsize) {
            try {
                System.out.println("<<<<<<<<<<<<<<<<<<<<<<LOADING BAY FULL, PRODUCTION PAUSED>>>>>>>>>>>>>>>>>>>>>");
                wait();
                Thread.sleep(4000);
            } catch (InterruptedException ex) {
            }
        }
    }

    synchronized public void unlock() throws InterruptedException {
        if (boxingQ.size() < globalvalues.boxingsize) {
            notify();
        }
    }
}

class generator extends Thread {
    //cangenerator 
    LinkedBlockingQueue<can> belt;
    LinkedBlockingQueue<box> boxingQ;
    LinkedBlockingQueue<box> ldbay;
    monitor m;
    boolean run = true;

    public generator(LinkedBlockingQueue<can> belt, LinkedBlockingQueue<box> boxingQ, LinkedBlockingQueue<box> ldbay, monitor m) {
        this.belt = belt;
        this.boxingQ = boxingQ;
        this.ldbay = ldbay;
        this.m = m;
    }

    @Override
    public void run() {
        int canid = 1;
        while (true) {
            try {
                m.lock(); //lock if loading bay full
                Thread.sleep(500);
                can c = new can(canid);
                System.out.println("ENTERED FACTORY:Can" + c.canid);
                ExecutorService exec = Executors.newCachedThreadPool();
                Future<can> dscan = exec.submit(new dscanner(c));
                canid++;
                if (dscan.get() == null) {
                    continue;
                }
                belt.put(c);
            } catch (InterruptedException | ExecutionException ex) {
            }
        }
    }
}

//dentscanner
class dscanner implements Callable<can> {

    Random rand = new Random();
    can c;

    public dscanner(can c) {
        this.c = c;
    }

    @Override
    public can call() throws Exception {
        while (true) {
            System.out.println("\tSCAN>>>>>>> Can" + c.canid);
            if (c.drand == 5) {
                System.out.println("\tStatus: Dented");
                System.out.println("\tCan" + c.canid + " Rejected\n");
                return null;
            } else {
                System.out.println("\tStatus:No Dents");
                System.out.println("\tPassing Can " + c.canid + " for sterilisation\n");
                return c;
            }
        }
    }
}

class sterilisation extends Thread {

    LinkedBlockingQueue<can> steril;
    LinkedBlockingQueue<can> presteril;
    LinkedBlockingQueue<can> belt;
    LinkedBlockingQueue<box> boxingQ;
    LinkedBlockingQueue<box> ldbay;
    monitor m;

    public sterilisation(LinkedBlockingQueue<can> steril, LinkedBlockingQueue<can> presteril, LinkedBlockingQueue<can> belt, LinkedBlockingQueue<box> boxingQ, LinkedBlockingQueue<box> ldbay, monitor m) {
        this.steril = steril;
        this.presteril = presteril;
        this.belt = belt;
        this.boxingQ = boxingQ;
        this.ldbay = ldbay;
        this.m = m;
    }

    public int getPrestrilQ() {
        return presteril.size();
    }

    @Override
    public void run() {
        while (true) {
            try {
                m.lock();
                can c = belt.take();
                Thread.sleep(500);
                presteril.put(c);
                //sterilise 4 at a time
                if (presteril.size() == globalvalues.sterilsize) {
                    for (int i = 1; i <= globalvalues.sterilsize; i++) {
                        c = presteril.take();
                        System.out.println("\t\t\tSTERLIZED:Can" + c.canid + "Sterilisation: (" + i + ")");
                        steril.put(c);
                    }
                }
            } catch (InterruptedException ex) {
            }
        }
    }
}

class filling extends Thread {

    LinkedBlockingQueue<can> steril;
    LinkedBlockingQueue<can> fill;
    LinkedBlockingQueue<box> boxingQ;
    monitor m;

    public filling(LinkedBlockingQueue<can> steril, LinkedBlockingQueue<can> fill, LinkedBlockingQueue<box> boxingQ, monitor m) {
        this.steril = steril;
        this.fill = fill;
        this.boxingQ = boxingQ;
        this.m = m;
    }

    @Override
    public void run() {
        while (true) {
            try {
                m.lock();
                Thread.sleep(200);
                can c = steril.take();
                System.out.println("\t\t\t\t\tFILLING >>>>Can" + c.canid);
                ExecutorService exec = Executors.newCachedThreadPool();
                Future<can> fscan = exec.submit(new fscanner(c));
                if (fscan.get() == null) {
                    continue;
                }
                fill.put(c);
            } catch (InterruptedException | ExecutionException ex) {
            }
        }
    }
}

//fillscanner
class fscanner implements Callable<can> {

    can c;

    public fscanner(can c) {
        this.c = c;
    }

    @Override
    public can call() throws Exception {
        while (true) {
            System.out.println("\t\t\t\t\tSCAN>>>>>>> Can" + c.canid);
            if (c.frand == 10) {
                System.out.println("\t\t\t\t\tStatus: Not Completely Filled");
                System.out.println("\t\t\t\t\tCan" + c.canid + " Rejected\n");
                return null;
            } else {
                System.out.println("\t\t\t\t\tStatus:Filled");
                System.out.println("\t\t\t\t\tPassing Can " + c.canid + " for sealing\n");
                return c;
            }
        }
    }
}

class sealing extends Thread {

    LinkedBlockingQueue<can> fill;
    LinkedBlockingQueue<can> seal;
    LinkedBlockingQueue<can> preseal;
    LinkedBlockingQueue<box> boxingQ;
    monitor m;

    public sealing(LinkedBlockingQueue<can> fill, LinkedBlockingQueue<can> seal, LinkedBlockingQueue<can> preseal, LinkedBlockingQueue<box> boxingQ, monitor m) {
        this.fill = fill;
        this.seal = seal;
        this.preseal = preseal;
        this.boxingQ = boxingQ;
        this.m = m;
    }

    public int getPresealQ() {
        return preseal.size();
    }

    @Override
    public void run() {
        while (true) {
            try {
                m.lock();
                Thread.sleep(800);
                can c = fill.take();
                preseal.put(c);
                //seal 12 at a time
                if (preseal.size() == globalvalues.sealsize) {
                    for (int i = 1; i <= globalvalues.sealsize; i++) {
                        c = preseal.take();
                        System.out.println("\t\t\t\t\t\t\t\t\tSEALING >>>Can" + c.canid + " Sealing:(" + i + ")");
                        ExecutorService exec = Executors.newCachedThreadPool();
                        Future<can> sscan = exec.submit(new sscanner(c));
                        if (sscan.get() == null) {
                            continue;
                        }
                        seal.put(c);
                    }
                }

            } catch (InterruptedException | ExecutionException ex) {
            }
        }

    }
}

//seal scanner
class sscanner implements Callable<can> {

    can c;

    public sscanner(can c) {
        this.c = c;
    }

    @Override
    public can call() throws Exception {
        while (true) {
            System.out.println("\t\t\t\t\t\t\t\t\tSCAN>>>>>>> Can" + c.canid);
            if (c.srand == 15) {
                System.out.println("\t\t\t\t\t\t\t\t\tStatus: Improper Sealing");
                System.out.println("\t\t\t\t\t\t\t\t\tCan" + c.canid + " Rejected\n");
                return null;
            } else {
                System.out.println("\t\t\t\t\t\t\t\t\tStatus:Sealed");
                System.out.println("\t\t\t\t\t\t\t\t\tPassing Can " + c.canid + " for labelling\n");
                return c;
            }
        }
    }
}

class labelling extends Thread {

    LinkedBlockingQueue<can> seal;
    LinkedBlockingQueue<can> label;
    LinkedBlockingQueue<box> boxingQ;
    monitor m;

    public labelling(LinkedBlockingQueue<can> seal, LinkedBlockingQueue<can> label, LinkedBlockingQueue<box> boxingQ, monitor m) {
        this.seal = seal;
        this.label = label;
        this.boxingQ = boxingQ;
        this.m = m;
    }

    @Override
    public void run() {
        while (true) {
            try {
                m.lock();
                Thread.sleep(200);
                can c = seal.take();
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\tLABELLING>>>>Can" + c.canid);
                ExecutorService exec = Executors.newCachedThreadPool();
                Future<can> lscan = exec.submit(new lscanner(c));
                if (lscan.get() == null) {
                    continue;
                }
                label.put(c);
            } catch (InterruptedException | ExecutionException ex) {
            }
        }
    }
}

class lscanner implements Callable<can> {

    can c;

    public lscanner(can c) {
        this.c = c;
    }

    @Override
    public can call() throws Exception {
        while (true) {
            System.out.println("\t\t\t\t\t\t\t\t\t\t\t\tSCAN>>>>>>> Can" + c.canid);
            if (c.lrand == 20) {
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\tStatus: Incorrect Labelling");
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\tCan" + c.canid + " Rejected\n");
                return null;
            } else {
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\tStatus: Labelled");
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\tPassing Can " + c.canid + " for packaging\n");
                return c;
            }
        }
    }
}

class packaging extends Thread {

    LinkedBlockingQueue<can> label;
    LinkedBlockingQueue<pack> packing;
    LinkedBlockingQueue<box> boxingQ;
    monitor m;
    int packid = 1;

    public packaging(LinkedBlockingQueue<can> label, LinkedBlockingQueue<pack> packing, LinkedBlockingQueue<box> boxingQ, monitor m) {
        this.label = label;
        this.packing = packing;
        this.boxingQ = boxingQ;
        this.m = m;
    }

    @Override
    public void run() {
        while (true) {
            try {
                m.lock();
                Thread.sleep(200);
                //pack 6 can into 1 pack
                LinkedBlockingQueue<can> packedcans = new LinkedBlockingQueue<>(globalvalues.perpacksize);
                for (int i = 1; i <= globalvalues.perpacksize; i++) {
                    can c = label.take();
                    System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tPACKING>>>> Can" + c.canid);
                    packedcans.put(c);
                }
                //creating pack object
                pack pk = new pack(packedcans, packid);
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tPACKAGING: (" + pk.packedcans.size() + ") cans have been shrinkwrapped");
                packing.put(pk);
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tPACKAGED>>>> Pack " + pk.packid);
                packid++;
            } catch (InterruptedException ex) {
            }
        }
    }
}

class boxing extends Thread {

    LinkedBlockingQueue<pack> packing;
    LinkedBlockingQueue<box> boxingQ;
    monitor m;
    int boxid = 1;

    public boxing(LinkedBlockingQueue<pack> packing, LinkedBlockingQueue<box> boxingQ, monitor m) {
        this.packing = packing;
        this.boxingQ = boxingQ;
        this.m = m;
    }

    public int getPackingQ() {
        return packing.size();
    }

    @Override
    public void run() {
        while (true) {
            try {
                m.lock();
                Thread.sleep(200);
                //box 27 packs into 1 box
                LinkedBlockingQueue<pack> packtobox = new LinkedBlockingQueue<>(globalvalues.packstobox);
                for (int i = 1; i <= globalvalues.packstobox; i++) {
                    pack pk = packing.take();
                    System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tBOXING>>>>>>>>>PACK " + pk.packid);
                    packtobox.put(pk);
                }
                //box object creation
                box bx = new box(packtobox, boxid);
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tBOXED: (" + bx.packtobox.size() + ") packs into 1 box");
                //Thread.sleep(1000);
                boxingQ.put(bx);
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tBOXED>>>>>>>>>BOX " + bx.boxid + "\n");
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tBOXES IN LOADING BAY>>>>>>(" + boxingQ.size() + ")");
                boxid++;

            } catch (InterruptedException ex) {
            }
        }
    }
}

class forklift extends Thread {

    LinkedBlockingQueue<box> boxingQ;
    LinkedBlockingQueue<box> ldbay;
    monitor m;
    int fid;

    public forklift(LinkedBlockingQueue<box> boxingQ, LinkedBlockingQueue<box> ldbay, monitor m, int fid) {
        this.boxingQ = boxingQ;
        this.ldbay = ldbay;
        this.m = m;
        this.fid = fid;
    }

    public int getBoxingQ() {
        return boxingQ.size();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(5000);
                box bx = boxingQ.take();
                m.unlock();
                System.out.println("\t\tFORKLIFT " + fid + " took BOX " + bx.boxid);
                Random rand = new Random();
                int repair = rand.nextInt(1);
                if (repair == 1 && repair == 3) {
                    System.out.println("\t\tFORKLIFT " + fid + " broken down, awaiting repair");
                    Thread.sleep(2000);
                    System.out.println("\t\tFORKLIFT " + fid + " repaired");
                    ldbay.put(bx);
                    System.out.println("\t\tFORKLIFT " + fid + " placed BOX " + bx.boxid + " in LOADING BAY");
                } else {
                    System.out.println("\t\tFORKLIFT " + fid + " no issues");
                    Thread.sleep(400);
                    ldbay.put(bx);
                    System.out.println("\t\tFORKLIFT " + fid + " placed BOX " + bx.boxid + " in LOADING BAY");
                }
            } catch (InterruptedException ex) {
            }
        }
    }
}

class loadingbay {

    Semaphore bay;

    public loadingbay(int slots) {
        bay = new Semaphore(slots);
    }

    public void enter(int id) {
        System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tLOADING BAY: Van " + id + " trying to enter");
        try {
            bay.acquire();
            System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tLOADING BAY: \t Van " + id + " entered");
        } catch (InterruptedException e) {
        }
    }

    public void exit(int id) {
        System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tLOADING BAY: Van " + id + " leaving");
        bay.release();
        System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tLOADING BAY: \t Van " + id + " left");
    }
}

class van extends Thread {

    loadingbay ld;
    int id;
    LinkedBlockingQueue<box> ldbay;
    LinkedBlockingQueue<devbox> vanQ;
    int bigboxid = 1;

    public van(loadingbay ld, int id, LinkedBlockingQueue<box> ldbay, LinkedBlockingQueue<devbox> vanQ) {
        this.ld = ld;
        this.id = id;
        this.ldbay = ldbay;
        this.vanQ = vanQ;
    }

    @Override
    public void run() {
        while (true) {
            try {
                LinkedBlockingQueue<box> twentybox = new LinkedBlockingQueue(globalvalues.finalbox);
                Thread.sleep(10000);
                //call semaphore to enter, van enter bay
                ld.enter(id);
                for (int i = 1; i <= globalvalues.finalbox; i++) {
                    box bx = ldbay.take();
                    twentybox.put(bx);
                }
                devbox dvb = new devbox(twentybox);
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tVAN " + id + " took " + twentybox.size() + " BOXES");
                vanQ.put(dvb);
                Thread.sleep(2000);
                
                //call semaphore to leave, van leaves bay
                ld.exit(id);
                Random rand = new Random();
                //random delivertime
                int delivertime = rand.nextInt(5);
                if (delivertime == 3) {
                    System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tVAN " + id + " taking too long");
                    Thread.sleep(2000);
                } else {
                    System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tVAN " + id + " delivering" + twentybox.size() + " BOXES");
                    Thread.sleep(500);
                }
                vanQ.take();
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\tVAN " + id + " delivered BOX " + twentybox.size() + " BOXES successfully");
            } catch (InterruptedException ex) {
            }
        }
    }
}
