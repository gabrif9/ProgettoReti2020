public class Timer implements Runnable {

    private MainServer mainServer;
    private static final int TIME_TO_BACKUP = 12000;

    public Timer(MainServer mainServer){
        this.mainServer = mainServer;
    }


    @Override
    public void run() {

        while (true){
            mainServer.setBackup();

            try {
                Thread.sleep(TIME_TO_BACKUP);
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }




    }
}
