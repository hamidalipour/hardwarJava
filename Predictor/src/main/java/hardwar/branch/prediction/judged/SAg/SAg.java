package hardwar.branch.prediction.judged.SAg;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAg implements BranchPredictor {
    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table
    private final Bit[] zeros;
    private final HashMode hashMode;

    public SAg() {
        this(4, 2, 8, 4);
    }

    public SAg(int BHRSize, int SCSize, int branchInstructionSize, int KSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = 0;
        this.KSize = KSize;
        this.hashMode = HashMode.XOR;

        // Initialize the PABHR with the given bhr and Ksize


        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"

        // Initialize the SC register

        // TODO : complete the constructor
        // Initialize the BHR register with the given size and no default value
        zeros = new Bit[SCSize];
        for (int i = 0; i < SCSize; i++) {
            zeros[i] = Bit.ZERO;
        }
        this.PSBHR = new RegisterBank(branchInstructionSize, BHRSize);
        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        this.PHT = new PageHistoryTable(((int)Math.pow(2.0,(double)BHRSize)),SCSize);
        // Initialize the SC register
        SC = new SIPORegister("SC",SCSize,null);
    }

    @Override
    public BranchResult predict(BranchInstruction instruction) {
        // TODO: complete Task 1
        ShiftRegister BHR = PSBHR.read(instruction.getInstructionAddress());
        PHT.putIfAbsent(BHR.read(),zeros);
        SC.load(PHT.get(BHR.read()));
        if(Bit.toNumber(SC.read())>=2){
            return BranchResult.TAKEN;
        }
        return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // TODO: complete Task 2
        ShiftRegister BHR = PSBHR.read(hash(branchInstruction.getInstructionAddress()));
        SC.load(PHT.get(BHR.read()));
        if(BranchResult.isTaken(actual)){
            PHT.put(BHR.read(),CombinationalLogic.count(SC.read(),true,CountMode.SATURATING));
            BHR.insert(Bit.ONE);

        }else {
            PHT.put(BHR.read(),CombinationalLogic.count(SC.read(),false,CountMode.SATURATING));
            BHR.insert(Bit.ZERO);
        }
        PSBHR.write(hash(branchInstruction.getInstructionAddress()), BHR.read());
    }

    private Bit[] getRBAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return hash(branchAddress);
    }

    /**
     * hash N bits to a K bit value
     *
     * @param bits program counter
     * @return hash value of fist M bits of `bits` in K bits
     */
    private Bit[] hash(Bit[] bits) {
        Bit[] hash = new Bit[KSize];

        // XOR the first M bits of the PC to produce the hash
        for (int i = 0; i < branchInstructionSize; i++) {
            int j = i % KSize;
            if (hash[j] == null) {
                hash[j] = bits[i];
            } else {
                Bit xorProduce = hash[j].getValue() ^ bits[i].getValue() ? Bit.ONE : Bit.ZERO;
                hash[j] = xorProduce;

            }
        }
        return hash;
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
        return null;
    }
}
