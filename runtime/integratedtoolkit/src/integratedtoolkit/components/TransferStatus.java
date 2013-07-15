
package integratedtoolkit.components;


//To inform about the status of a transfer
public interface TransferStatus {

	enum TransferState {
		DONE,
		FAILED;
	}
	
	void fileTransferInfo(int transferId, TransferState status, String message);
	
}
