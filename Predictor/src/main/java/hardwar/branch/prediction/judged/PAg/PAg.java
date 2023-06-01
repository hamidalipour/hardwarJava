package hardwar.branch.prediction.judged.PAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAg implements BranchPredictor {
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final Bit[] zeros;

    public PAg() {
        this(4, 2, 8);
    }

    /**
     * Creates a new PAg predictor with the given BHR register size and initializes the PABHR based on
     * the branch instruction size and BHR size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public PAg(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
        zeros = new Bit[SCSize];
        for (int i = 0; i < SCSize; i++) {
            zeros[i] = Bit.ZERO;
        }
        this.PABHR = new RegisterBank((int)Math.pow(2.0,(double)BHRSize), BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        this.PHT = new PageHistoryTable(((int)Math.pow(2.0,(double)BHRSize)),SCSize);
        // Initialize the SC register
        SC = new SIPORegister("SC",SCSize,null);
    }

    /**
     * @param instruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction instruction) {
        // TODO : complete Task 1
        ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
        PHT.putIfAbsent(BHR.read(),zeros);
        SC.load(PHT.get(BHR.read()));
        if(Bit.toNumber(SC.read())>=2){
            return BranchResult.TAKEN;
        }
        return BranchResult.NOT_TAKEN;
    }

    /**
     * @param instruction the branch instruction
     * @param actual      the actual result of branch (taken or not)
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
        SC.load(PHT.get(BHR.read()));
        if(BranchResult.isTaken(actual)){
            PHT.put(BHR.read(),CombinationalLogic.count(SC.read(),true,CountMode.SATURATING));
            BHR.insert(Bit.ONE);

        }else {
            PHT.put(BHR.read(),CombinationalLogic.count(SC.read(),false,CountMode.SATURATING));
            BHR.insert(Bit.ZERO);
        }
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAg predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
