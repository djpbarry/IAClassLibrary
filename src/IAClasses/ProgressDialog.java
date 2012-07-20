/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ProgressDialog.java
 *
 * Created on 20-May-2011, 18:21:13
 */

package IAClasses;

/**
 *
 * @author barry05
 */
public class ProgressDialog extends javax.swing.JDialog {
    private String label;

    /** Creates new form ProgressDialog */
    public ProgressDialog(java.awt.Frame parent, String label, boolean modal, boolean alwaysOnTop) {
        super(parent, modal);
        this.label = label;
        setAlwaysOnTop(alwaysOnTop);
        initComponents();
    }

    public void updateProgress(int current, int max){
        bar.setMinimum(0);
        bar.setMaximum(max);
        bar.setValue(current);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        bar = new javax.swing.JProgressBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.FlowLayout());

        jLabel1.setText(label);
        getContentPane().add(jLabel1);
        getContentPane().add(bar);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JProgressBar bar;
    private javax.swing.JLabel jLabel1;
    // End of variables declaration//GEN-END:variables

}
