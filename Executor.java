import mpi.Graphcomm;
import mpi.Intracomm;
import mpi.MPI;

public class Executor {
	public static int N;
	public static int P;
	public static int H;

	public static void main(String[] args) {
		P = Integer.parseInt(args[1]);
		N = Integer.parseInt(args[3]);
		H = N / P;

		MPI.Init(args);
		int[] index = new int[] { 4, 5, 6, 7, 11, 12, 13, 14 };
		int[] edges = new int[] { 1, 2, 3, 4, 0, 0, 0, 0, 5, 6, 7, 4, 4, 4 };
		Graphcomm graph = MPI.COMM_WORLD.Create_graph(index, edges, false);

		int[][] sendBuf = new int[8 * N + 2 * (H * 8) + 8][N];

		int[][] recvBuf = new int[N + H + H + 1][N];

		int[][] MC_send = new int[N][N];
		int[][] MM_send = new int[N][N];
		int[][] MB_send = new int[N][N];
		int alfa;

		int[][] MC_recv = new int[H][N];
		int[][] MM_recv = new int[H][N];
		int[][] MB_recv = new int[N][N];

		int[][] MA_send = new int[H][N];
		int[][] MA_resv = new int[N][N];

		if (graph.Rank() == 0) {
			for (int i = 0; i < N; i++) {
				for (int j = 0; j < N; j++) {
					MB_send[i][j] = 1;
					MC_send[i][j] = 1;
					MM_send[i][j] = 1;
				}
			}

			MM_send[3][3] = 5;
			int i = 0;
			int g = 0;
			int y = 0;
			for (int j = 0; j < graph.Size(); j++) {

				// write MB
				for (int j2 = 0; j2 < N; j2++, i++) {
					for (int k = 0; k < N; k++) {
						sendBuf[i][k] = MB_send[j2][k];

					}

				}

				// write mc
				for (; g < H * (j + 1); g++, i++) {

					for (int j2 = 0; j2 < MC_send.length; j2++) {
						sendBuf[i][j2] = MC_send[g][j2];

					}

				}

				// write mm
				for (; y < H * (j + 1); y++, i++) {
					for (int j2 = 0; j2 < MM_send.length; j2++) {
						sendBuf[i][j2] = MM_send[y][j2];

					}

				}
				sendBuf[i][0] = 1;
				i++;

			}

			// matrixOutput(sendBuf);

		}

		graph.Scatter(sendBuf, 0, N + 2 * H + 1, MPI.OBJECT, recvBuf, 0, N + 2
				* H + 1, MPI.OBJECT, 0);

		int i = 0;

		// get MB
		for (; i < MB_recv.length; i++) {
			for (int j = 0; j < MB_recv[i].length; j++) {
				MB_recv[i][j] = recvBuf[i][j];
			}
		}

		// getMM
		for (int j = 0; j < MC_recv.length; j++, i++) {
			for (int k = 0; k < MC_recv[j].length; k++) {
				MC_recv[j][k] = recvBuf[i][k];
			}
		}

		// getMM
		for (int j = 0; j < MM_recv.length; j++, i++) {
			for (int k = 0; k < MM_recv[j].length; k++) {
				MM_recv[j][k] = recvBuf[i][k];
			}
		}
		alfa = recvBuf[i][0];

		for (int j = 0; j < H; j++) {
			for (int k = 0; k < N; k++) {
				MA_send[j][k] = 0;
				for (int m = 0; m < N; m++) {
					MA_send[j][k] += MC_recv[j][m] * MB_recv[m][k];
				}
				MA_send[j][k] += MM_recv[j][k] * alfa;
			}
		}
		
		
		graph.Gather(MA_send, 0, H, MPI.OBJECT, MA_resv, 0, H, MPI.OBJECT, 0);

		if (graph.Rank() == 0) {
			System.out.println("Result");
			matrixOutput(MA_resv);
		}

		MPI.Finalize();

	}

	public static void grapfTopoTest(Graphcomm comm) {
		if (comm.Rank() == 0) {
			for (int rank = 0; rank < 8; rank++) {
				System.out.println("I am node " + rank + " my neighbors is:");
				for (int nbrs : comm.Neighbours(rank)) {
					System.out.print(nbrs + ", ");
				}
				System.out.println();
				System.out.println();
			}
		}
	}

	public static void matrixOutput(int[][] array) {
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < array[i].length; j++) {
				System.out.print(array[i][j] + ", ");
			}
			System.out.println();
		}
		System.out.println();
	}

}
