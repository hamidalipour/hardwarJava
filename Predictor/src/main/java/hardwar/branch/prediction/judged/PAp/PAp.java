package hardwar.branch.prediction.judged.PAp;


import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;
import java.lang.Math;

import java.lang.reflect.Array;
import java.util.*;


public class PAp implements BranchPredictor {

    private final int branchInstructionSize;

    private final ShiftRegister SC; // saturating counter register

    private final RegisterBank PABHR; // per address branch history register
    private final Bit[] zeros;

    private final Cache<Bit[], Bit[]> PAPHT; // Per Address Predication History Table

    public PAp() {
        this(4, 2, 8);
    }

    public PAp(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        zeros = new Bit[SCSize];
        for (int i = 0; i < SCSize; i++) {
            zeros[i] = Bit.ZERO;
        }
        this.branchInstructionSize = branchInstructionSize;

        // Initialize the BHR register with the given size and no default value
        this.PABHR = new RegisterBank(branchInstructionSize, BHRSize);

        // Initializing the PAPHT with BranchInstructionSize as PHT Selector and 2^BHRSize row as each PHT entries
        // number and SCSize as block size
        PAPHT = new PageHistoryTable(((int)Math.pow(2.0,(double)(BHRSize+branchInstructionSize))),SCSize);

        // Initialize the SC register
        SC = new SIPORegister("SC",SCSize,null);
    }

    @Override
    public BranchResult predict(BranchInstruction branchInstruction) {
        ShiftRegister BHR = PABHR.read(branchInstruction.getInstructionAddress());
        PAPHT.putIfAbsent(getCacheEntry(branchInstruction.getInstructionAddress()),zeros);
        SC.load(PAPHT.get(getCacheEntry(branchInstruction.getInstructionAddress())))\;
        if(Bit.toNumber(SC.read()) > (int) Math.pow(2.0 , SC.getLength() - 1) - 1){
            return BranchResult.TAKEN;
        }
        return BranchResult.NOT_TAKEN;
    }

    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
        SC.load(PAPHT.get(getCacheEntry(instruction.getInstructionAddress())));
        if(BranchResult.isTaken(actual)){
            PAPHT.put(getCacheEntry(instruction.getInstructionAddress()),CombinationalLogic.count(SC.read(),true,CountMode.SATURATING));
            BHR.insert(Bit.ONE);

        }else {
            PAPHT.put(getCacheEntry(instruction.getInstructionAddress()),CombinationalLogic.count(SC.read(),false,CountMode.SATURATING));
            BHR.insert(Bit.ZERO);
        }
    }


    private Bit[] getCacheEntry(Bit[] branchAddress, Bit[] BHRValue) {
        // Concatenate the branch address bits with the BHR bits
        Bit[] cacheEntry = new Bit[branchAddress.length + BHRValue.length];
        System.arraycopy(branchAddress, 0, cacheEntry, 0, branchInstructionSize);
        System.arraycopy(BHRValue, 0, cacheEntry, branchAddress.length, BHRValue.length);
        return cacheEntry;
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
        return "PAp predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PAPHT.monitor();
    }
}
