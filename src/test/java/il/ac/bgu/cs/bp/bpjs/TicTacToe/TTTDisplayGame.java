package il.ac.bgu.cs.bp.bpjs.TicTacToe;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.hamcrest.core.IsInstanceOf;

import il.ac.bgu.cs.bp.bpjs.TicTacToe.events.Click;
import il.ac.bgu.cs.bp.bpjs.TicTacToe.events.Move;
import il.ac.bgu.cs.bp.bpjs.TicTacToe.events.StaticEvents;
import il.ac.bgu.cs.bp.bpjs.TicTacToe.events.X;
import il.ac.bgu.cs.bp.bpjs.bprogram.runtimeengine.BProgram;
import il.ac.bgu.cs.bp.bpjs.bprogram.runtimeengine.BProgramRunner;
import il.ac.bgu.cs.bp.bpjs.bprogram.runtimeengine.listeners.BProgramListenerAdapter;
import il.ac.bgu.cs.bp.bpjs.events.BEvent;

/**
 * Class that implements the Graphical User Interface for the game
 */
public class TTTDisplayGame implements ActionListener {
	private BProgram bp;
	private BProgramRunner rnr;
	
	private JFrame window = new JFrame("Tic-Tac-Toe");
	public JButton buttons[][] = new JButton[3][];
	public JLabel message = new JLabel();
	
	/**
	 * Constructor.
	 * @param rnr 
	 */

	public TTTDisplayGame(BProgram bp, BProgramRunner rnr) {

		this.bp = bp;
		this.rnr = rnr;
		
		// Create window
		window.setSize(150, 150);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.setLayout(new BorderLayout());
		window.setLocation(new Point(600,100)); 

		// The board
		JPanel board = new JPanel();
		board.setLayout(new GridLayout(3, 3));
		// The message label
		message.setHorizontalAlignment(SwingConstants.CENTER);

		// Create buttons
		for (int i = 0; i < 3; i++) {
			buttons[i] = new JButton[3];
			for (int j = 0; j < 3; j++) {
				buttons[i][j] = new TTTButton(i, j);
				board.add(buttons[i][j]);
				buttons[i][j].addActionListener(this);
			}
		}

		// Add the board and the message component to the window
		window.add(board, BorderLayout.CENTER);
		window.add(message, BorderLayout.SOUTH);

		// Make the window visible
		window.setVisible(true);
		
		//Writs 'X' and 'O' on the buttons
		rnr.addListener( new BProgramListenerAdapter() {

			@Override
			public void eventSelected(BProgram bp, BEvent theEvent) {
				if(theEvent instanceof Move){
			
					Move mv = (Move)theEvent;
					buttons[mv.row][mv.col].setText(mv.displayString());
//					if(mv instanceof X)
//						bp.enqueueExternalEvent(new Click(1,1));		
				}	
				else {
					String msg = "";	
					if (theEvent == StaticEvents.XWin)
						msg = "X Wins!";
					else if (theEvent == StaticEvents.OWin)
						msg = "O Wins!";
					else if (theEvent == StaticEvents.draw )
						msg = "It's a Draw!";
					System.out.println(msg);
					message.setText(msg);
				}
			}
			
		});

	}

	/**
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent a) {
		final TTTButton btt = ((TTTButton) a.getSource());		
		bp.enqueueExternalEvent(new Click(btt.row,btt.col));
}

/**
 * A button that remembers its position on the board
 */
@SuppressWarnings("serial")
class TTTButton extends JButton {
	int row;
	int col;

	/**
	 * Constructor.
	 *
	 * @param row
	 *            The row of the button.
	 * @param col
	 *            The column of the button.
	 */
	public TTTButton(int row, int col) {
		super();
		this.row = row;
		this.col = col;
	}

}
}