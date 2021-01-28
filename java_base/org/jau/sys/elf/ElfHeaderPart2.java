/**
 * Author: Sven Gothel <sgothel@jausoft.com>
 * Copyright (c) 2021 Gothel Software e.K.
 * Copyright (c) 2013 Gothel Software e.K.
 * Copyright (c) 2013 JogAmp Community.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.jau.sys.elf;

import static org.jau.sys.elf.IOUtils.readBytes;
import static org.jau.sys.elf.IOUtils.seek;
import static org.jau.sys.elf.IOUtils.shortToInt;
import static org.jau.sys.elf.IOUtils.toHexString;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.jau.sys.PlatformTypes.ABIType;
import org.jau.sys.PlatformTypes.CPUFamily;
import org.jau.sys.PlatformTypes.CPUType;

/**
 * ELF ABI Header Part-2
 * <p>
 * Part-2 can only be read w/ knowledge of CPUType!
 * </p>
 * <p>
 * References:
 * <ul>
 *   <li>http://www.sco.com/developers/gabi/latest/contents.html</li>
 *   <li>https://en.wikipedia.org/wiki/Executable_and_Linkable_Format</li>
 *   <li>http://linux.die.net/man/5/elf</li>
 *   <li>http://infocenter.arm.com/
 *   <ul>
 *      <li>ARM IHI 0044E, current through ABI release 2.09</li>
 *      <li>ARM IHI 0056B: Elf for ARM 64-bit Architecture</li>
 *   </ul></li>
 * </ul>
 * </p>
 */
public class ElfHeaderPart2 {
    /**
     * This masks an 8-bit version number, the version of the ABI to which this
     * ELF file conforms. This ABI is version 5. A value of 0 denotes unknown conformance.
     * {@value}
     */
    public static final int EF_ARM_ABIMASK  = 0xFF000000;
    public static final int EF_ARM_ABISHIFT  = 24;

    /**
     * ARM ABI version 5.
     * {@value}
     */
    public static final int EF_ARM_ABI5  = 0x05000000;

    /**
     * The ELF file contains BE-8 code, suitable for execution on an ARM
     * Architecture v6 processor. This flag must only be set on an executable file.
     * {@value}
     */
    public static final int EF_ARM_BE8      = 0x00800000;

    /**
     * Legacy code (ABI version 4 and earlier) generated by gcc-arm-xxx might
     * use these bits.
     * {@value}
     */
    public static final int EF_ARM_GCCMASK  = 0x00400FFF;

    /**
     * Set in executable file headers (e_type = ET_EXEC or ET_DYN) to note that
     * the executable file was built to conform to the hardware floating-point
     * procedure-call standard.
     * <p>
     * Compatible with legacy (pre version 5) gcc use as EF_ARM_VFP_FLOAT.
     * </p>
     * <p>
     * Note: This is not used (anymore)
     * </p>
     * {@value}
     */
    public static final int EF_ARM_ABI_FLOAT_HARD  = 0x00000400;

    /**
     * Set in executable file headers (e_type = ET_EXEC or ET_DYN) to note
     * explicitly that the executable file was built to conform to the software
     * floating-point procedure-call standard (the base standard). If both
     * {@link #EF_ARM_ABI_FLOAT_HARD} and {@link #EF_ARM_ABI_FLOAT_SOFT} are clear,
     * conformance to the base procedure-call standard is implied.
     * <p>
     * Compatible with legacy (pre version 5) gcc use as EF_ARM_SOFT_FLOAT.
     * </p>
     * <p>
     * Note: This is not used (anymore)
     * </p>
     * {@value}
     */
    public static final int EF_ARM_ABI_FLOAT_SOFT  = 0x00000200;

    /** Public access to the elf header part-1 (CPU/ABI independent read) */
    public final ElfHeaderPart1 eh1;

    /** Public access to the raw elf header part-2 (CPU/ABI dependent read) */
    public final Ehdr_p2 raw;

    /** Lower case CPUType name */
    public final String cpuName;
    public final CPUType cpuType;
    public final ABIType abiType;

    /** Public access to the {@link SectionHeader} */
    public final SectionHeader[] sht;

    /**
     * Note: The input stream shall stay untouch to be able to read sections!
     *
     * @param in input stream of a binary file at position zero
     * @return
     * @throws IOException if reading from the given input stream fails or less then ELF Header size bytes
     * @throws IllegalArgumentException if the given input stream does not represent an ELF Header
     */
    public static ElfHeaderPart2 read(final ElfHeaderPart1 eh1, final RandomAccessFile in) throws IOException, IllegalArgumentException {
        return new ElfHeaderPart2(eh1, in);
    }

    /**
     * @param buf ELF Header bytes
     * @throws IllegalArgumentException if the given buffer does not represent an ELF Header
     * @throws IOException
     */
    ElfHeaderPart2(final ElfHeaderPart1 eh1, final RandomAccessFile in) throws IllegalArgumentException, IOException {
        this.eh1 = eh1;
        //
        // Part-2
        //
        {
            final byte[] buf = new byte[Ehdr_p2.size(eh1.machDesc.ordinal())];
            readBytes (in, buf, 0, buf.length);
            final ByteBuffer eh2Bytes = ByteBuffer.wrap(buf, 0, buf.length);
            raw = Ehdr_p2.create(eh1.machDesc.ordinal(), eh2Bytes);
        }
        sht = readSectionHeaderTable(in);

        if( CPUFamily.ARM32 == eh1.cpuType.family ) {
            // AArch64, has no SHT_ARM_ATTRIBUTES or SHT_AARCH64_ATTRIBUTES SectionHeader defined in our builds!
            String armCpuName = null;
            String armCpuRawName = null;
            boolean abiVFPArgsAcceptsVFPVariant = false;
            final SectionHeader sh = getSectionHeader(SectionHeader.SHT_ARM_ATTRIBUTES);
            if(ElfHeaderPart1.DEBUG) {
                System.err.println("ELF-2: Got ARM Attribs Section Header: "+sh);
            }
            if( null != sh ) {
                final SectionArmAttributes sArmAttrs = (SectionArmAttributes) sh.readSection(in);
                if(ElfHeaderPart1.DEBUG) {
                    System.err.println("ELF-2: Got ARM Attribs Section Block : "+sArmAttrs);
                }
                final SectionArmAttributes.Attribute cpuNameArgsAttr = sArmAttrs.get(SectionArmAttributes.Tag.CPU_name);
                if( null != cpuNameArgsAttr && cpuNameArgsAttr.isNTBS() ) {
                    armCpuName = cpuNameArgsAttr.getNTBS();
                }
                final SectionArmAttributes.Attribute cpuRawNameArgsAttr = sArmAttrs.get(SectionArmAttributes.Tag.CPU_raw_name);
                if( null != cpuRawNameArgsAttr && cpuRawNameArgsAttr.isNTBS() ) {
                    armCpuRawName = cpuRawNameArgsAttr.getNTBS();
                }
                final SectionArmAttributes.Attribute abiVFPArgsAttr = sArmAttrs.get(SectionArmAttributes.Tag.ABI_VFP_args);
                if( null != abiVFPArgsAttr ) {
                    abiVFPArgsAcceptsVFPVariant = SectionArmAttributes.abiVFPArgsAcceptsVFPVariant(abiVFPArgsAttr.getULEB128());
                }
            }
            {
                String _cpuName;
                CPUType _cpuType;
                if( null != armCpuName && armCpuName.length() > 0 ) {
                    _cpuName = armCpuName.toLowerCase().replace(' ', '-');
                    _cpuType = queryCPUTypeSafe(_cpuName, true /* isARM */); // 1st-try: native name
                } else if( null != armCpuRawName && armCpuRawName.length() > 0 ) {
                    _cpuName = armCpuRawName.toLowerCase().replace(' ', '-');
                    _cpuType = queryCPUTypeSafe(_cpuName, true /* isARM */); // 1st-try: native name
                } else {
                    _cpuName = eh1.cpuName;
                    _cpuType = eh1.cpuType;
                }

                if( null == _cpuType ) {
                    // 2nd-try: "arm-" + native name
                    _cpuName = "arm-"+_cpuName;
                    _cpuType = queryCPUTypeSafe(_cpuName, true /* isARM */);
                    if( null == _cpuType ) {
                        // finally: Use ELF-1, impossible path due to above 'arm-' prefix
                        _cpuName = eh1.cpuName;
                        _cpuType = queryCPUTypeSafe(_cpuName, true /* isARM */);
                        if( null == _cpuType ) {
                            throw new InternalError("XXX: "+_cpuName+", "+eh1); // shall not happen
                        }
                    }
                }
                cpuName = _cpuName;
                cpuType = _cpuType;
                if(ElfHeaderPart1.DEBUG) {
                    System.err.println("ELF-2: abiARM cpuName "+_cpuName+"[armCpuName "+armCpuName+", armCpuRawName "+armCpuRawName+"] -> "+cpuName+" -> "+cpuType+", abiVFPArgsAcceptsVFPVariant "+abiVFPArgsAcceptsVFPVariant);
                }
            }
            abiType = abiVFPArgsAcceptsVFPVariant ? ABIType.EABI_GNU_ARMHF : ABIType.EABI_GNU_ARMEL;
        } else {
            // Non CPUFamily.ARM32
            cpuName = eh1.cpuName;
            cpuType = eh1.cpuType;
            abiType = eh1.abiType;
        }
        if(ElfHeaderPart1.DEBUG) {
            System.err.println("ELF-2: cpuName "+cpuName+" -> "+cpuType+", "+abiType);
        }
    }
    private static CPUType queryCPUTypeSafe(final String cpuName, final boolean isARM) {
        try {
            final CPUType res = CPUType.query(cpuName);
            if( isARM && null != res ) {
                if( res.family != CPUFamily.ARM32 && res.family != CPUFamily.ARM64 ) {
                    if(ElfHeaderPart1.DEBUG) {
                        System.err.println("ELF-2: queryCPUTypeSafe("+cpuName+", isARM="+isARM+") -> "+res+" ( "+res.family+" ) rejected");
                    }
                    return null; // reject non-arm
                }
            }
            return res;
        } catch (final Throwable t) {
            if(ElfHeaderPart1.DEBUG) {
                System.err.println("ELF-2: queryCPUTypeSafe("+cpuName+", isARM="+isARM+"): "+t.getMessage());
                t.printStackTrace();
            }
        }
        return null;
    }

    public final short getSize() { return raw.getE_ehsize(); }

    /** Returns the processor-specific flags associated with the file. */
    public final int getFlags() {
        return raw.getE_flags();
    }

    /** Returns the ARM EABI version from {@link #getFlags() flags}, maybe 0 if not an ARM EABI. */
    public byte getArmABI() {
        return (byte) ( ( ( EF_ARM_ABIMASK & raw.getE_flags() ) >> EF_ARM_ABISHIFT ) & 0xff );
    }

    /** Returns the ARM EABI legacy GCC {@link #getFlags() flags}, maybe 0 if not an ARM EABI or not having legacy GCC flags. */
    public int getArmLegacyGCCFlags() {
        final int f = raw.getE_flags();
        return 0 != ( EF_ARM_ABIMASK & f ) ? ( EF_ARM_GCCMASK & f ) : 0;
    }

    /**
     * Returns the ARM EABI float mode from {@link #getFlags() flags},
     * i.e. 1 for {@link #EF_ARM_ABI_FLOAT_SOFT}, 2 for {@link #EF_ARM_ABI_FLOAT_HARD}
     * or 0 for none.
     * <p>
     * Note: This is not used (anymore)
     * </p>
     */
    public byte getArmFloatMode() {
        final int f = raw.getE_flags();
        if( 0 != ( EF_ARM_ABIMASK & f ) ) {
            if( ( EF_ARM_ABI_FLOAT_HARD & f ) != 0 ) {
                return 2;
            }
            if( ( EF_ARM_ABI_FLOAT_SOFT & f ) != 0 ) {
                return 1;
            }
        }
        return 0;
    }

    /** Returns the 1st occurence of matching SectionHeader {@link SectionHeader#getType() type}, or null if not exists. */
    public final SectionHeader getSectionHeader(final int type) {
        for(int i=0; i<sht.length; i++) {
            final SectionHeader sh = sht[i];
            if( sh.getType() == type ) {
                return sh;
            }
        }
        return null;
    }

    /** Returns the 1st occurence of matching SectionHeader {@link SectionHeader#getName() name}, or null if not exists. */
    public final SectionHeader getSectionHeader(final String name) {
        for(int i=0; i<sht.length; i++) {
            final SectionHeader sh = sht[i];
            if( sh.getName().equals(name) ) {
                return sh;
            }
        }
        return null;
    }

    @Override
    public final String toString() {
        final int armABI = getArmABI();
        final String armFlagsS;
        if( 0 != armABI ) {
            armFlagsS=", arm[abi "+armABI+", lGCC "+getArmLegacyGCCFlags()+", float "+getArmFloatMode()+"]";
        } else {
            armFlagsS="";
        }
        return "ELF-2["+cpuType+", "+abiType+", flags["+toHexString(getFlags())+armFlagsS+"], sh-num "+sht.length+"]";
    }

    final SectionHeader[] readSectionHeaderTable(final RandomAccessFile in) throws IOException, IllegalArgumentException {
        // positioning
        {
            final long off = raw.getE_shoff(); // absolute offset
            if( 0 == off ) {
                return new SectionHeader[0];
            }
            seek(in, off);
        }
        final SectionHeader[] sht;
        final int strndx = raw.getE_shstrndx();
        final int size = raw.getE_shentsize();
        final int num;
        int i;
        if( 0 == raw.getE_shnum() ) {
            // Read 1st table 1st and use it's sh_size
            final byte[] buf0 = new byte[size];
            readBytes(in, buf0, 0, size);
            final SectionHeader sh0 = new SectionHeader(this, buf0, 0, size, 0);
            num = (int) sh0.raw.getSh_size();
            if( 0 >= num ) {
                throw new IllegalArgumentException("EHdr sh_num == 0 and 1st SHdr size == 0");
            }
            sht = new SectionHeader[num];
            sht[0] = sh0;
            i=1;
        } else {
            num = raw.getE_shnum();
            sht = new SectionHeader[num];
            i=0;
        }
        for(; i<num; i++) {
            final byte[] buf = new byte[size];
            readBytes(in, buf, 0, size);
            sht[i] = new SectionHeader(this, buf, 0, size, i);
        }
        if( SectionHeader.SHN_UNDEF != strndx ) {
            // has section name string table
            if( shortToInt(SectionHeader.SHN_LORESERVE) <= strndx ) {
                throw new InternalError("TODO strndx: "+SectionHeader.SHN_LORESERVE+" < "+strndx);
            }
            final SectionHeader strShdr = sht[strndx];
            if( SectionHeader.SHT_STRTAB != strShdr.getType() ) {
                throw new IllegalArgumentException("Ref. string Shdr["+strndx+"] is of type "+strShdr.raw.getSh_type());
            }
            final Section strS = strShdr.readSection(in);
            for(i=0; i<num; i++) {
                sht[i].initName(strS, sht[i].raw.getSh_name());
            }
        }

        return sht;
    }
}
