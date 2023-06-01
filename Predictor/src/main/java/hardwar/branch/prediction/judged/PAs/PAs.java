package hardwar.branch.prediction.judged.PAs;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAs implements BranchPredictor {

    private final int branchInstructionSize;
    private final int KSize;
    private final HashMode hashMode;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address Branch History Register
    private final Cache<Bit[], Bit[]> PSPHT; // Per Set Predication History Table
    private final Bit[] zeros;


    public PAs() {
        this(4, 2, 8, 4, HashMode.XOR);
    }

    public PAs(int BHRSize, int SCSize, int branchInstructionSize, int KSize, HashMode hashMode) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;
        this.hashMode = HashMode.XOR;

        // Initialize the PABHR with the given bhr and branch instruction size

        // Initializing the PAPHT with K bit as PHT selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size

        // Initialize the saturating counter
        zeros = new Bit[SCSize];
        for (int i = 0; i < SCSize; i++) {
            zeros[i] = Bit.ZERO;
        }
        this.PABHR = new RegisterBank(branchInstructionSize, BHRSize);
        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        this.PSPHT = new PageHistoryTable(((int)Math.pow(2.0,(double)BHRSize+branchInstructionSize)),SCSize);
        // Initialize the SC register
        SC = new SIPORegister("SC",SCSize,null);
    }

    /**
     * predicts the result of a branch instruction based on the per address BHR and hash value of branch
     * instruction address
     *
     * @param branchInstruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        ShiftRegister BHR = PABHR.read(branchInstruction.getInstructionAddress());
        getCacheEntry(branchInstruction.getInstructionAddress(),BHR.read());
        PSPHT.putIfAbsent(getCacheEntry(branchInstruction.getInstructionAddress(),BHR.read()),zeros);
        SC.load(PSPHT.get(getCacheEntry(branchInstruction.getInstructionAddress(),BHR.read())));
        if(Bit.toNumber(SC.read())>=2){
            return BranchResult.TAKEN;
        }
        return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        // TODO:complete Task 2
        ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
        SC.load(PSPHT.get(  getCacheEntry(instruction.getInstructionAddress(),BHR.read())));
        if(BranchResult.isTaken(actual)){
            PSPHT.put( getCacheEntry(instruction.getInstructionAddress(),BHR.read()),CombinationalLogic.count(SC.read(),true,CountMode.SATURATING));
            BHR.insert(Bit.ONE);

        }else {
            PSPHT.put( getCacheEntry(instruction.getInstructionAddress(),BHR.read()),CombinationalLogic.count(SC.read(),false,CountMode.SATURATING));
            BHR.insert(Bit.ZERO);
        }
        PABHR.write(instruction.getInstructionAddress(), BHR.read());
    }

    @Override
    public String monitor() {
        return "PAs predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PSPHT.monitor();
    }

    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // hash the branch address
        Bit[] hashKSize = CombinationalLogic.hash(branchAddress, KSize, hashMode);

        // Concatenate the Hash bits with the BHR bits
        Bit[] cacheEntry = new Bit[hashKSize.length + BHRValue.length];
        System.arraycopy(hashKSize, 0, cacheEntry, 0, hashKSize.length);
        System.arraycopy(BHRValue, 0, cacheEntry, hashKSize.length, BHRValue.length);

        return cacheEntry;
    }


    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }
}
