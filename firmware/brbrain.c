/****************************************************************************
*
*   Bioloid Remote Brain CM-5 firmware
*
*   Copyright (c) 2007 Marsette Vona
*
*   This program is free software; you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation; either version 2 of the License, or
*   (at your option) any later version.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with this program; if not, write to the Free Software
*   Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*
*   $Id: brbrain.c 24 2008-05-02 22:08:41Z vona $
****************************************************************************/

#include <inttypes.h>
#include <avr/io.h>
#include <avr/interrupt.h>
#include <util/delay.h>
#include "CBUF.h"

#define DBG(c) txPC(c, 0)
//#define DBG(c)

/* comms baud rate constants */
#define BAUDRATE_1000000 1
#define BAUDRATE_38400   51 /* at 16MHz (0.2% error) */
#define BAUDRATE_57600   34 /* at 16MHz (-0.8% error) */
#define BAUDRATE_76800   25 /* at 16MHz (0.2% error) */
#define BAUDRATE_115200  16 /* at 16MHz (2.1% error) */
#define BAUDRATE_250000  7  /* at 16MHz (0.0% error) */

/* dynamixel bus on UART0 */
#define DYNAMIXEL_BAUDRATE BAUDRATE_1000000

/* pc link on UART1 */
#define PC_BAUDRATE BAUDRATE_115200
/* #define PC_BAUDRATE BAUDRATE_57600 */
/* #define PC_BAUDRATE BAUDRATE_38400 */

/* max non-broadcast dynamixel ID */
#define MAX_DYNAMIXEL_ID 253 

/* broadcast dynamixel ID */
#define ID_BCAST 0xfe

/* max num dynamixels in a format */
#define MAX_DYNAMIXELS  32

/* virtual dynamixel register holding error code */
#define VIRTUAL_ERROR_REG_ADDR 54

/* address of last dynamixel register, including the virtual one */
#define MAX_START_ADDR 54

/* total number of bytes in dynamixel register bank including virtual */
#define NUM_REG_BYTES  55

/* max number of bytes readable in one block from a dynamixel */
#define MAX_DYNAMIXEL_BLOCK_READ 20

/* the formats */
#define F_READ  0
#define F_WRITE 1

static uint8_t numDynamixels[2] = { 0, 0 };
static uint16_t totalNumBytes[2] = { 0, 0 };
static uint8_t axID[2][MAX_DYNAMIXELS];
static uint8_t startAddr[2][MAX_DYNAMIXELS];
static uint8_t numBytes[2][MAX_DYNAMIXELS];

/* buffer for params to/from dynamixel */
static uint8_t dynamixelParams[64];

/* buffer sizes WARNING these must be a power of 2 */
#define dynamixelRxBuffer_SIZE 128 
#define dynamixelTxBuffer_SIZE 128 
#define pcRxBuffer_SIZE 128
#define pcTxBuffer_SIZE 128

/* dynamixel CBUFs */
#if (dynamixelRxBuffer_SIZE > 128)
typedef uint16_t dynamixelRxIndex_t;
#else
typedef uint8_t dynamixelRxIndex_t;
#endif
volatile struct {
  dynamixelRxIndex_t m_getIdx;
  dynamixelRxIndex_t m_putIdx;
  uint8_t m_entry[dynamixelRxBuffer_SIZE];
} dynamixelRxBuffer;

#if (dynamixelTxBuffer_SIZE > 128)
typedef uint16_t dynamixelTxIndex_t;
#else
typedef uint8_t dynamixelTxIndex_t;
#endif
volatile struct {
  dynamixelTxIndex_t m_getIdx;
  dynamixelTxIndex_t m_putIdx;
  uint8_t m_entry[dynamixelTxBuffer_SIZE];
} dynamixelTxBuffer;

/* pc CBUFs */
#if (pcRxBuffer_SIZE > 128)
typedef uint16_t pcRxIndex_t;
#else
typedef uint8_t pcRxIndex_t;
#endif
volatile struct {
  pcRxIndex_t m_getIdx;
  pcRxIndex_t m_putIdx;
  uint8_t m_entry[pcRxBuffer_SIZE];
} pcRxBuffer;

#if (pcTxBuffer_SIZE > 128)
typedef uint16_t pcTxIndex_t;
#else
typedef uint8_t pcTxIndex_t;
#endif
volatile struct {
  pcTxIndex_t m_getIdx;
  pcTxIndex_t m_putIdx;
  uint8_t m_entry[pcTxBuffer_SIZE];
} pcTxBuffer;

/* tx/rx checksums */
static uint8_t checksumTxDynamixel = 0;
static uint8_t checksumRxDynamixel = 0;
static uint8_t checksumTxPC = 0;
static uint8_t checksumRxPC = 0;

#define DYNAMIXEL_INSTRUCTION_CHECKSUM_ERROR (1<<4)
#define MAX_DYNAMIXEL_TRIES 4
static uint8_t numDynamixelRetries = 0;

/* port pins */

#define BIT_DYNAMIXEL_TXD              PE2
#define BIT_DYNAMIXEL_RXD              PE3

#define BIT_ZIGBEE_LED                 PD1
#define BIT_ZIGBEE_RESET               PD4  /* out : default 1 */
#define BIT_ENABLE_RXD_LINK_PC         PD5  /* out : default 1 */
#define BIT_ENABLE_RXD_LINK_ZIGBEE     PD6  /* out : default 0 */
#define BIT_LINK_PLUGIN                PD7  /* in, no pull up */
#define BIT_CHARGE                     PB5  /* out, 0 = charge */

#define BIT_BLUETOOTH_RTS             BIT_ZIGBEE_LED
#define BIT_BLUETOOTH_CTS             BIT_ZIGBEE_RESET
#define BIT_ENABLE_RXD_LINK_BLUETOOTH BIT_ENABLE_RXD_LINK_ZIGBEE

#define BLUETOOTH_ACTIVE (PORTD&_BV(BIT_ENABLE_RXD_LINK_BLUETOOTH))

/* dynamixel bus is half-duplex TTL */
#define DYNAMIXEL_RXD \
  PORTE &= ~_BV(BIT_DYNAMIXEL_TXD),PORTE |= _BV(BIT_DYNAMIXEL_RXD)
#define DYNAMIXEL_TXD \
  PORTE &= ~_BV(BIT_DYNAMIXEL_RXD),PORTE |= _BV(BIT_DYNAMIXEL_TXD)

#define DYNAMIXEL_TXD_DONE (!(UCSR0B&_BV(UDRIE0)))

/* ADC channels */
#define CHANNEL_POS   0
#define CHANNEL_NEG   1
#define CHANNEL_THERM 2
#define CHANNEL_GND   31

/* most recently read ADC values */
static volatile uint8_t adcValue[] = {0, 0, 0};

/*
 * define this to enable battery charging 
 *
 * WARNING charge code is currently EXPERIMENTAL and does not yet incorporate
 * temperature monitoring
 *
 * work-around is to temporarily flash standard firmware from robotis, charge
 * battery, and then re-flash brbrain firmware
 *
 * improper battery charging could damage battery and robot or even cause fire
 *
 * see full DISCLAIMER.txt included in orginal package download
 */
/* #define ENABLE_CHARGING */

/* charging starts when enabled and neg voltage is above 1.10V */
#define CHARGE_START_THRESHOLD 13

/* charging ends when neg voltage is below 0.95V */
#define CHARGE_COMPLETE_THRESHOLD 11

/* do an ADC conversion, result in ADCH */
#define CONVERT_AND_WAIT(channel) {             \
    ADMUX &= 0xf0;                              \
    ADMUX |= channel & 0x0f;                    \
    ADCSRA |= (1 <<ADSC);                       \
    while (ADCSRA & (1<<ADSC))                  \
      ;                                         \
  }

/* LEDs */
#define LED_PWR     _BV(PC0)
#define LED_TXD     _BV(PC1)
#define LED_RXD     _BV(PC2)
#define LED_AUX     _BV(PC3)
#define LED_MANAGE  _BV(PC4)
#define LED_PROGRAM _BV(PC5)
#define LED_PLAY    _BV(PC6)
#define LED_ALL     0x7f

#define LED_OFF(mask) PORTC |= mask
#define LED_ON(mask)  PORTC &= ~mask

/* LED mapping */
#define LED_RXD_DYNAMIXEL    LED_MANAGE
#define LED_TXD_DYNAMIXEL    LED_PROGRAM
#define LED_RXD_PC           LED_RXD
#define LED_TXD_PC           LED_TXD
#define LED_ERROR            LED_PLAY
#define LED_IDLE             LED_AUX
#define LED_BLUETOOTH_ACTIVE LED_MANAGE
#define LED_BLUETOOTH_RTS    LED_PROGRAM

/* pushbuttons */
#define PB_UP    _BV(PE4)
#define PB_DOWN  _BV(PE5)
#define PB_LEFT  _BV(PE6)
#define PB_RIGHT _BV(PE7)
#define PB_START _BV(PD0)

#define PB_ALL (PB_UP|PB_DOWN|PB_LEFT|PB_RIGHT|PB_START)

/* mask is composed of PB_* constants, return mask is 1 for each btn pressed */
#define PB(mask) (mask&(((~PINE)&0xf0)|((~PIND)&0x01)))

/* button mapping */
#define PB_TOGGLE_RX_BLUETOOTH_PC PB_START
#define PB_TOGGLE_CHARGE_ENABLE   PB_UP

/* CM-5 to PC packet instructions */
#define I_STATUS 0xfa
#define I_DATA   0xfb

/* CM-5 to dynamixel packet insructions */
#define D_I_PING       0x01
#define D_I_READ_DATA  0x02
#define D_I_WRITE_DATA 0x03
#define D_I_REG_WRITE  0x04
#define D_I_ACTION     0x05
#define D_I_RESET      0x06
#define D_I_SYNC_WRITE 0x83

/* CM-5 status flags */
#define S_PC_TIMEOUT                 (1<<0)
#define S_DYNAMIXEL_TIMEOUT          (1<<1)
#define S_INVALID_PC_COMMAND         (1<<2)
#define S_INVALID_DYNAMIXEL_RESPONSE (1<<3)
#define S_PC_RX_OVERFLOW             (1<<4)
#define S_DYNAMIXEL_RX_OVERFLOW      (1<<5)
#define S_PC_CHECKSUM_ERROR          (1<<6)
#define S_DYNAMIXEL_CHECKSUM_ERROR   (1<<7)

/* CM-5 status, bitmask of S_* flags */
static uint8_t status = 0;

/* ~10ms ticks to go until timeout */
static volatile uint8_t ticksToGo = 0;

#define RX_PC_TIMEOUT_TICKS        100
#define RX_DYNAMIXEL_TIMEOUT_TICKS 10
#define TX_DYNAMIXEL_TIMEOUT_TICKS 100
#define RX_CLEAR_DELAY_TICKS       50

static uint8_t enableRxPCTimeout = 1;
static uint8_t enableRxDynamixelTimeout = 1;

static volatile uint8_t bluetoothTogglePending = 0;

static volatile uint8_t chargeEnableTogglePending = 0;
static volatile uint8_t chargeEnabled = 0;
static volatile uint8_t charging = 0;

#define CHARGE_PHASE 0
#define MEASURE_PHASE 1

/* #define CHARGE_PHASE_TICKS  3200 */
#define CHARGE_PHASE_TICKS  400 
#define MEASURE_PHASE_TICKS 100
#define MEASURE_TICKS       50

static volatile uint8_t chargePhase = CHARGE_PHASE;
static volatile uint16_t chargePhaseTicksToGo = CHARGE_PHASE_TICKS;

/* set a read or write format */
static void setFormat(uint8_t f);

/* command handlers */
static void handlePing();
static void handleSetReadFormat();
static void handleSetWriteFormat();
static void handleReadData();
static void handleWriteData();
static void handleInvalid();

/* pointer to a function taking no parameters and returning nothing */
typedef void (*handler_t)();

/* command handlers indexed by low nybble of instruction */
static handler_t commandHandler[] = {
  /* 0 */ handlePing,           /* I_PING */
  /* 1 */ handleSetReadFormat,  /* I_SET_READ_FORMAT */
  /* 2 */ handleSetWriteFormat, /* I_SET_WRITE_FORMAT */
  /* 3 */ handleReadData,       /* I_READ_DATA */
  /* 4 */ handleWriteData,      /* I_WRITE_DATA */
  /* 5 */ handleInvalid,
  /* 6 */ handleInvalid,
  /* 7 */ handleInvalid,
  /* 8 */ handleInvalid,
  /* 9 */ handleInvalid,
  /* a */ handleInvalid,
  /* b */ handleInvalid,
  /* c */ handleInvalid,
  /* d */ handleInvalid,
  /* e */ handleInvalid,
  /* f */ handleInvalid
};

/* handle commands indefinitely */
static void commandLoop();

/* set a flag in the status byte, with interrupts disabled */
static void setStatusFlag(uint8_t flag);

/* clear a flag in the status byte, with interrupts disabled */
static void clearStatusFlag(uint8_t flag);

/* rx checksum from PC and validate it, return 0 on timeout */
static uint8_t endRXPacketPC();

/* send instruction to pc */
static void startTXPacketPC(uint8_t instruction);

/* send checksum to pc */
static void endTXPacketPC();

/* tx a status packet to the PC */
static void txStatusPC();

/* tx the adc values to the PC */
static void txADCValuesPC();

/* rx packet header from dynamixel, return 0 on timeout */
static uint8_t startRXPacketDynamixel(uint8_t *id,
                                      uint8_t *numParams,
                                      uint8_t *error);

/* rx checksum from dynamixel and validate it, return 0 on timeout */
static uint8_t endRXPacketDynamixel();

/* receive a packet from dynamixel bus, return 0 on timeout */
static uint8_t rxPacketDynamixel(uint8_t *id,
                                 uint8_t *numParams,
                                 uint8_t *error,
                                 uint8_t *params,
                                 uint8_t numParamsExpected);

/* send packet header to dynamixel */
static void startTXPacketDynamixel(uint8_t id,
                                   uint8_t numParams,
                                   uint8_t instruction);

/* send checksum to dynamixel, wait for all tx complete, return 0 on timeout */
static uint8_t endTXPacketDynamixel();

/* clr stat flg & ret 1 iff either checksum fail and still have tries left **/
static uint8_t tryDynamixelAgain(uint8_t dynamixelError);

/* setup peripherals */
static void initHardware();

/* tx to dynamixel bus, blocking */
static void txDynamixel(uint8_t byte, uint8_t addToChecksum);

/* tx to pc, blocking */
static void txPC(uint8_t byte, uint8_t addToChecksum);

/* rx from dx bus, blocking, does _not_ control bus dir, ret 0 on timeout */
static uint8_t rxDynamixel(uint8_t *byte, uint8_t addToChecksum);

/* rx from pc, blocking, return 0 on timeout */
static uint8_t rxPC(uint8_t *byte, uint8_t addToChecksum);

/* check number of bytes waiting in rx buf from dynamixel bus */
/* static dynamixelRxIndex_t numWaitingDynamixel(); */

/* check number of bytes waiting in rx buf from pc */
/* static pcRxIndex_t numWaitingPC(); */

/* clear rx buf from dynamixel bus */
static void clearRxBufDynamixel();

/* read the relevant ADC channels into adcValue */
static void readADCs();

/* clear rx buf from pc */
static void clearRxBufPC();

int main(void) {

  initHardware();

  LED_ON(LED_PWR);

  sei();

  commandLoop();

  return 0;
}

static void handlePing() {

  uint8_t addr;

  if (rxPC(&addr, 1) && endRXPacketPC()) {
    
    if (addr <= 253) {

      uint8_t rxID = 0xff;
      uint8_t rxError = 0xff;

      /* retry loop */
      do {

        startTXPacketDynamixel(addr, 0, D_I_PING);
        
        if (!endTXPacketDynamixel())
          break;
          
      } while (rxPacketDynamixel(&rxID, 0, &rxError, 0, 0) &&
               tryDynamixelAgain(rxError));

      if (rxID != addr)
        setStatusFlag(S_INVALID_DYNAMIXEL_RESPONSE);
    }
    
    if (addr == 254)
      setStatusFlag(S_INVALID_PC_COMMAND);

    /* addr == 255 means ping the CM-5 itself */
  }

  txStatusPC();
}

static void handleSetReadFormat() {
  setFormat(F_READ);
}
static void handleSetWriteFormat() {
  setFormat(F_WRITE);
}

static void setFormat(uint8_t f) {

  uint8_t n;
  uint16_t totalBytes = 0;

  numDynamixels[f] = 0;
  totalNumBytes[f] = 0;

  if (!rxPC(&n, 1))
    goto DONE;

  if (n > MAX_DYNAMIXELS) {
    setStatusFlag(S_INVALID_PC_COMMAND);
    goto DONE;
  }

  for (uint8_t i = 0; i < n; i++) {

    uint8_t id, start, n;

    if (!rxPC(&id, 1))
      goto DONE;

    if (id > MAX_DYNAMIXEL_ID) {
      setStatusFlag(S_INVALID_PC_COMMAND);
      goto DONE;
    }

    if (!rxPC(&start, 1))
      goto DONE;
    
    if (start > MAX_START_ADDR) {
      setStatusFlag(S_INVALID_PC_COMMAND);
      goto DONE;
    }

    if (!rxPC(&n, 1))
      goto DONE;
    
    if (n > (NUM_REG_BYTES-start)) {
      setStatusFlag(S_INVALID_PC_COMMAND);
      goto DONE;
    }

    axID[f][i] = id;
    startAddr[f][i] = start;
    numBytes[f][i] = n;

    totalBytes += n;
  }

  totalNumBytes[f] = totalBytes;
  numDynamixels[f] = n;

  endRXPacketPC();
  
 DONE:
  txStatusPC();
}

static void handleReadData() {

  uint8_t rxPCok = endRXPacketPC();

  uint16_t bytesToGo = totalNumBytes[F_READ];

  startTXPacketPC(I_DATA);

  if (!rxPCok)
    goto DONE;
    
  for (uint8_t i = 0; i < numDynamixels[F_READ]; i++) {

    uint8_t id = axID[F_READ][i];
    uint8_t start = startAddr[F_READ][i];
    uint8_t n = numBytes[F_READ][i];

    uint8_t rxID, rxN, rxError;

    uint8_t returnError = 0;

    uint8_t thisN;

    /* read in blocks of up to MAX_DYNAMIXEL_BLOCK_READ bytes */

    while (n > 0) {

      thisN = (n <= MAX_DYNAMIXEL_BLOCK_READ) ? n : MAX_DYNAMIXEL_BLOCK_READ;
     
      /* do we need to read the virtual reg? */
      if ((start+thisN) == VIRTUAL_ERROR_REG_ADDR) {
        returnError = 1;
        thisN--;
      }

      for (uint8_t j = 0; j < thisN; j++)
        dynamixelParams[j] = 0xff;
      
      rxID = rxN = rxError = 0xff;
        
      /* retry loop */
      do {

        /* send read request packet */
        
        if (thisN > 0) {
          
          /* still need to read some non-virtual regs */
          
          startTXPacketDynamixel(id, 2, D_I_READ_DATA);
          txDynamixel(start, 1);
          txDynamixel(thisN, 1);
          
        } else {
          
          /* only reading the virtual reg (error) */
          
          startTXPacketDynamixel(id, 0, D_I_PING);
        }
        
        if (!endTXPacketDynamixel())
          goto DONE;
        
      } while (rxPacketDynamixel(&rxID, &rxN, &rxError, dynamixelParams, thisN)
               && tryDynamixelAgain(rxError));

      /* fwd read data */

      if ((rxID == id) && (rxN == thisN)) {
        
        /* recv pkt ok, though maybe timed out during params */
        
        for (uint8_t j = 0; j < thisN; j++) {
          txPC(dynamixelParams[j], 1);
          bytesToGo--;
        }

      } else {
        
        /* recv pkt from dynamixel bad, stuff return pkt */
        
        setStatusFlag(S_INVALID_DYNAMIXEL_RESPONSE);
        
        for (uint8_t j = 0; j < thisN; j++) {
          txPC(0xff, 1);
          bytesToGo--;
        }
      }

      /* return the virtual reg? */
      if (returnError) {
        txPC(rxError, 1);
        bytesToGo--;
        n--;
        start++;
      }

      n -= thisN;
      start += thisN;

    } /* for each block */
  } /* for each dynamixel */
 
 DONE: 

  /* stuff the return pkt as necessary */
  while (bytesToGo)
    txPC(0xff, 1);

  txPC(status, 1);
  txPC(numDynamixelRetries, 1);

  txADCValuesPC();
  
  endTXPacketPC();
}

static void handleWriteData() {

  for (uint8_t i = 0; i < numDynamixels[F_WRITE]; i++) {

    uint8_t id = axID[F_WRITE][i];
    uint8_t start = startAddr[F_WRITE][i];
    uint8_t n = numBytes[F_WRITE][i];

    uint8_t rxID = 0xff;
    uint8_t rxError = 0xff;

    for (uint8_t j = 0; j < n; j++) {
      if (!rxPC(&(dynamixelParams[j]), 1))
        goto DONE;
    }

    /* retry loop (requires dynamixel status return level = 2) */
    do {

      startTXPacketDynamixel(id, n+1, D_I_REG_WRITE);
      
      txDynamixel(start, 1);

      for (uint8_t j = 0; j < n; j++) 
        txDynamixel(dynamixelParams[j], 1);
      
      if (!endTXPacketDynamixel())
        goto DONE;

    } while (rxPacketDynamixel(&rxID, 0, &rxError, 0, 0) &&
             tryDynamixelAgain(rxError));

    if (rxID != id)
      setStatusFlag(S_INVALID_DYNAMIXEL_RESPONSE);
  }

  /* TBD do this redundantly since we can't verify checksums? */
  startTXPacketDynamixel(ID_BCAST, 0, D_I_ACTION);
  if (!endTXPacketDynamixel())
    goto DONE;
  
  endRXPacketPC();

 DONE:
  txStatusPC();
}

static void handleInvalid() {

  setStatusFlag(S_INVALID_PC_COMMAND);
  txStatusPC();

  ticksToGo = RX_CLEAR_DELAY_TICKS;
  while (ticksToGo)
    ;

  clearRxBufPC();
  clearRxBufDynamixel();
}

static void commandLoop() {

  uint8_t instruction;
  handler_t handler;

  for (;;) {

    LED_ON(LED_IDLE);

    cli();
    status = 0;
    sei();

    numDynamixelRetries = 0;
    checksumRxPC = 0;
    enableRxPCTimeout = 0;

    while (!rxPC(&instruction, 1))
      ;

    enableRxPCTimeout = 1;

    LED_OFF(LED_IDLE);

    handler = handleInvalid;

    if ((instruction&0xf0) == 0xf0)
      handler = commandHandler[instruction&0x0f];

    handler();
  }
}

static void setStatusFlag(uint8_t flag) {
  cli();
  status |= flag;
  sei();
}

static void clearStatusFlag(uint8_t flag) {
  cli();
  status &= ~flag;
  sei();
}

static uint8_t endRXPacketPC() {

  uint8_t checksum;

  if (!rxPC(&checksum, 0))
    return 0;

  //THIS IS BORKED
  //http://www.nongnu.org/avr-libc/user-manual/FAQ.html#faq_intpromote
  //if (checksum ^ (~checksumRxPC))
  if (checksum != ((uint8_t)(~checksumRxPC)))
    setStatusFlag(S_PC_CHECKSUM_ERROR);

  return 1;
}

static void startTXPacketPC(uint8_t instruction) {
  checksumTxPC = 0;
  txPC(instruction, 1);
}

static void endTXPacketPC() {
  txPC(~checksumTxPC, 0);
}

static void txStatusPC() {

  startTXPacketPC(I_STATUS);

  txPC(status, 1);
  txPC(numDynamixelRetries, 1);

  txADCValuesPC();

  endTXPacketPC();
}

static void txADCValuesPC() {
  cli();
  txPC(adcValue[CHANNEL_POS], 1);
  txPC(adcValue[CHANNEL_NEG], 1);
  txPC(adcValue[CHANNEL_THERM], 1);
  sei();
}

static uint8_t startRXPacketDynamixel(uint8_t *id,
                                      uint8_t *numParams,
                                      uint8_t *error) {
  checksumRxDynamixel = 0;

  uint8_t byte;

  for (uint8_t i = 0; i < 2; i++) {

    if (!rxDynamixel(&byte, 0))
      return 0;
    
    if (byte != 0xff)
      setStatusFlag(S_INVALID_DYNAMIXEL_RESPONSE);
  }

  if (!rxDynamixel(&byte, 1))
    return 0;
  
  if (id)
    *id = byte;
  
  if (!rxDynamixel(&byte, 1))
    return 0;
 
  if (numParams)
    *numParams = byte-2;

  if (!rxDynamixel(&byte, 1))
    return 0;

  if (error)
    *error = byte;

  return 1;
}

static uint8_t endRXPacketDynamixel() {

  uint8_t checksum;

  if (!rxDynamixel(&checksum, 0))
    return 0;

  //THIS IS BORKED
  //http://www.nongnu.org/avr-libc/user-manual/FAQ.html#faq_intpromote
  //if (checksum != (~checksumRxPC))
  if (checksum != ((uint8_t)~checksumRxDynamixel))
    setStatusFlag(S_DYNAMIXEL_CHECKSUM_ERROR);

  return 1;
}

static uint8_t rxPacketDynamixel(uint8_t *id,
                                 uint8_t *numParams,
                                 uint8_t *error,
                                 uint8_t *params,
                                 uint8_t numParamsExpected) {
  uint8_t actualNumParams;

  if (!startRXPacketDynamixel(id, &actualNumParams, error))
    return 0;

  if (numParams)
    *numParams = actualNumParams;

  uint8_t byte;

  for (uint8_t i = 0; i < actualNumParams; i++) {

    if (!rxDynamixel(&byte, 1))
      return 0;

    if (params && (i < numParamsExpected))
      params[i] = byte;
  }

  if (!endRXPacketDynamixel())
    return 0;

  if (actualNumParams != numParamsExpected)
    setStatusFlag(S_INVALID_DYNAMIXEL_RESPONSE);

  return 1;
}

static void startTXPacketDynamixel(uint8_t id,
                                   uint8_t numParams,
                                   uint8_t instruction) {
  checksumTxDynamixel = 0;

  txDynamixel(0xff, 0);
  txDynamixel(0xff, 0);

  txDynamixel(id, 1);
  txDynamixel(numParams+2, 1);
  txDynamixel(instruction, 1);
}

static uint8_t endTXPacketDynamixel() {

  txDynamixel(~checksumTxDynamixel, 0);

  ticksToGo = TX_DYNAMIXEL_TIMEOUT_TICKS;

  while (!CBUF_IsEmpty(dynamixelTxBuffer) || !DYNAMIXEL_TXD_DONE) {
    if (!ticksToGo) {
      setStatusFlag(S_DYNAMIXEL_TIMEOUT);
      LED_ON(LED_ERROR);
      return 0;
    }
  }

  return 1;
}

 static uint8_t tryDynamixelAgain(uint8_t dynamixelError) {

   if (((dynamixelError&DYNAMIXEL_INSTRUCTION_CHECKSUM_ERROR) ||
        (status&S_DYNAMIXEL_CHECKSUM_ERROR)) &&
       (numDynamixelRetries < (MAX_DYNAMIXEL_TRIES-1))) {
    numDynamixelRetries++;
    clearStatusFlag(S_DYNAMIXEL_CHECKSUM_ERROR);
    return 1; 
  } 

  return 0;
}

static void initHardware() {

  /* NOTE: even if we don't think we're using some of this hardware, we should
   * probably still init the port pins for it to be on the safe side.
   */

  /* inputs */

  DDRA = DDRB = DDRC = DDRD = DDRE = DDRF = 0;  /* set all ports to input */
  PORTB = PORTC = PORTD = PORTE = PORTF = PORTG = 0x00;

#if MCU == atmega128
  SFIOR &= ~_BV(PUD); //enable pullups
#elif MCU == atmega2561
  MCUCR &= ~_BV(PUD); //enable pullups
#else
  #error "MCU has an unrecognized value."
#endif
  
  /* pullups on pushbuttons */
  PORTE |= PB_UP|PB_DOWN|PB_LEFT|PB_RIGHT;
  PORTD |= PB_START;

  /* no pullup on BIT_LINK_PLUGIN */

  /* outputs */

  DDRC |= LED_ALL;
  LED_OFF(LED_ALL);

  DDRE |= _BV(BIT_DYNAMIXEL_RXD)|_BV(BIT_DYNAMIXEL_TXD);
  DDRD |= _BV(BIT_ENABLE_RXD_LINK_PC)|_BV(BIT_ENABLE_RXD_LINK_ZIGBEE);

  /* zigbee led bit is now bluetooth rts signal, and it's an input */
  DDRD |= _BV(BIT_ZIGBEE_RESET)/*_BV(BIT_ZIGBEE_LED)*/; 

  /* zigbee reset is now bluetooth cts signal, and it needs to be low */
  /*   PORTD |= _BV(BIT_ZIGBEE_RESET); */

#ifdef INITIAL_RXD_BLUETOOTH

  /* initially listen to the bluetooth */
  PORTD |= _BV(BIT_ENABLE_RXD_LINK_BLUETOOTH);
  LED_ON(LED_BLUETOOTH_ACTIVE);

#else

  /* initially listen to the pc */
  PORTD |= _BV(BIT_ENABLE_RXD_LINK_PC);

#endif

  DYNAMIXEL_RXD; /* initially listen on the dynamixel bus */

  DDRB |= _BV(BIT_CHARGE);
  PORTB |= _BV(BIT_CHARGE); /* not charging */

  /* external AREF (connected to 5V), left adj result so ADCH is high byte **/
  ADMUX = _BV(ADLAR);

  /* enable ADC and divide 16MHz by 128 for 125kHz adc clock (~0.1ms conv) */
  ADCSRA = _BV(ADEN)|_BV(ADPS2)|_BV(ADPS1)|_BV(ADPS0);

  /* do a dummy conversion */
  CONVERT_AND_WAIT(CHANNEL_GND);

  readADCs();

  /* UART0: half-duplex dynamixel bus */
  UBRR0H = 0;
  UBRR0L = DYNAMIXEL_BAUDRATE;
  UCSR0A = _BV(U2X0); /* double speed */
  UCSR0B = _BV(TXEN0)|_BV(RXEN0)|_BV(RXCIE0);
  UCSR0C = _BV(UCSZ01)|_BV(UCSZ00); /* 8N1 */

  /* UART1: pc */
  UBRR1H = 0;
  UBRR1L = PC_BAUDRATE;
  UCSR1A = _BV(U2X1); /* double speed */
  UCSR1B = _BV(TXEN1)|_BV(RXEN1)|_BV(RXCIE1);
  UCSR1C = _BV(UCSZ11)|_BV(UCSZ10); /* 8N1 */

  /* counter 0 interrupt every ~10ms */
#if MCU == atmega128
  TCCR0 = _BV(WGM01); /* mode 2, Clear Timer on Compare match */
  TCCR0 |= _BV(CS02)|_BV(CS01)|_BV(CS00); /* divide by 1024 */
  TIMSK |= _BV(OCIE0); /* OCR interrupt enable */
  OCR0 = 156; /* 16M/1024 = 15625Hz; 15625/156 = 100.16Hz */
#elif MCU == atmega2561
  TCCR0B = _BV(WGM01); /* mode 2, Clear Timer on Compare match */
  TCCR0B |= _BV(CS02)|_BV(CS01)|_BV(CS00); /* divide by 1024 */
  TIMSK0 |= _BV(OCIE0B); /* OCR interrupt enable */
  OCR1A = 156; /* 16M/1024 = 15625Hz; 15625/156 = 100.16Hz */
#else
  #error "MCU has an unrecognized value."
#endif
}

static void txDynamixel(uint8_t byte, uint8_t addToChecksum) {

  DYNAMIXEL_TXD;

  LED_ON(LED_TXD_DYNAMIXEL);

  while (CBUF_IsFull(dynamixelTxBuffer))
    ;

  CBUF_Push(dynamixelTxBuffer, byte);

  UCSR0B |= _BV(UDRIE0);

  //  /* wait until tx reg is empty */
  //  loop_until_bit_is_set(UCSR0A, UDRE0);
  //
  //  UDR0 = byte;

  if (addToChecksum)
    checksumTxDynamixel += byte;
}

static void txPC(uint8_t byte, uint8_t addToChecksum) {

  LED_ON(LED_TXD_PC);

  while (CBUF_IsFull(pcTxBuffer))
    ;

  CBUF_Push(pcTxBuffer, byte);

  UCSR1B |= _BV(UDRIE1);

  //  /* wait until tx reg is empty */
  //  loop_until_bit_is_set(UCSR1A, UDRE1);
  //
  //  UDR1 = byte;

  if (addToChecksum)
    checksumTxPC += byte;
}

static uint8_t rxDynamixel(uint8_t *byte, uint8_t addToChecksum) {

  DYNAMIXEL_RXD;

  ticksToGo = RX_DYNAMIXEL_TIMEOUT_TICKS;

  while (CBUF_IsEmpty(dynamixelRxBuffer)) {

    if (enableRxDynamixelTimeout && !ticksToGo) {
      setStatusFlag(S_DYNAMIXEL_TIMEOUT);
      return 0;
    }
  }
  
  uint8_t b = CBUF_Pop(dynamixelRxBuffer);

  if (byte)
    *byte = b;

  if (addToChecksum)
    checksumRxDynamixel += b;

  return 1;
}

static uint8_t rxPC(uint8_t *byte, uint8_t addToChecksum) {

  ticksToGo = RX_PC_TIMEOUT_TICKS;

  while (CBUF_IsEmpty(pcRxBuffer)) {
    if (enableRxPCTimeout && !ticksToGo) {
      setStatusFlag(S_PC_TIMEOUT);
      return 0;
    }
  }
  
  uint8_t b = CBUF_Pop(pcRxBuffer);

  if (byte)
    *byte = b;

  if (addToChecksum)
    checksumRxPC += b;

  return 1;
}

/*
static dynamixelRxIndex_t numWaitingDynamixel() {
  return CBUF_Len(dynamixelRxBuffer);
}

static pcRxIndex_t numWaitingPC() {
  return CBUF_Len(pcRxBuffer);
}
*/

static void clearRxBufDynamixel() {
  CBUF_Init(dynamixelRxBuffer);
}

static void clearRxBufPC() {
  CBUF_Init(pcRxBuffer);
}

static void readADCs() {

  CONVERT_AND_WAIT(CHANNEL_POS);
  adcValue[CHANNEL_POS] = ADCH;

  CONVERT_AND_WAIT(CHANNEL_NEG);
  adcValue[CHANNEL_NEG] = ADCH;

  CONVERT_AND_WAIT(CHANNEL_THERM);
  adcValue[CHANNEL_THERM] = ADCH;
}

/* rx from dynamixel */
#if MCU == atmega128
  ISR(SIG_UART0_RECV) {
#elif MCU == atmega2561
  ISR(SIG_USART0_RECV) {
#else
  #error "MCU has an unrecognized value."
#endif
  LED_ON(LED_RXD_DYNAMIXEL);

  uint8_t byte = UDR0;

  if (!CBUF_IsFull(dynamixelRxBuffer)) {
    CBUF_Push(dynamixelRxBuffer, byte);
  } else {
    status |= S_DYNAMIXEL_RX_OVERFLOW;
    LED_ON(LED_ERROR);
  }
}

/* tx buf to dynamixel empty */
#if MCU == atmega128
  ISR(SIG_UART0_DATA) {
#elif MCU == atmega2561
  ISR(SIG_USART0_DATA) {
#else
  #error "MCU has an unrecognized value."
#endif
  if (CBUF_IsEmpty(dynamixelTxBuffer)) {

    UCSR0B &= ~_BV(UDRIE0);

  } else {

    UDR0 = CBUF_Pop(dynamixelTxBuffer);

  }
}

/* rx from PC */
#if MCU == atmega128
  ISR(SIG_UART1_RECV) {
#elif MCU == atmega2561
  ISR(SIG_USART1_RECV) {
#else
  #error "MCU has an unrecognized value."
#endif
  LED_ON(LED_RXD_PC);

  uint8_t byte = UDR1;

  if (!CBUF_IsFull(pcRxBuffer)) {
    CBUF_Push(pcRxBuffer, byte);
  } else {
    status |= S_PC_RX_OVERFLOW;
    LED_ON(LED_ERROR);
  }
}

/* tx buf to pc empty */
#if MCU == atmega128
  ISR(SIG_UART1_DATA) {
#elif MCU == atmega2561
  ISR(SIG_USART1_DATA) {
#else
  #error "MCU has an unrecognized value."
#endif
  if (CBUF_IsEmpty(pcTxBuffer)) {

    UCSR1B &= ~_BV(UDRIE1);

  } else {

    if (!BLUETOOTH_ACTIVE || !(PORTD&_BV(BIT_BLUETOOTH_RTS)))
      UDR1 = CBUF_Pop(pcTxBuffer);

    /* if we skip sending this byte because the bt module had its RTS raised,
       we will try again either at the next txPC() or at the next 10ms tick
       that occurs with RTS low */
  }
}

/** we get this interrupt every ~10 ms **/
#if MCU == atmega128
  ISR(SIG_OUTPUT_COMPARE0) {
#elif MCU == atmega2561
  ISR(SIG_OUTPUT_COMPARE0A) {
#else
  #error "MCU has an unrecognized value."
#endif
  if (ticksToGo)
    ticksToGo--;

  if (CBUF_IsEmpty(pcRxBuffer))
    LED_OFF(LED_RXD_PC);
  
  if (CBUF_IsEmpty(pcTxBuffer))
      LED_OFF(LED_TXD_PC);

  if (!BLUETOOTH_ACTIVE) {
    
    if (CBUF_IsEmpty(dynamixelRxBuffer))
      LED_OFF(LED_RXD_DYNAMIXEL);
    
    if (CBUF_IsEmpty(dynamixelTxBuffer))
      LED_OFF(LED_TXD_DYNAMIXEL);

  }  else {

    if (PORTD&_BV(BIT_BLUETOOTH_RTS))
      LED_ON(LED_BLUETOOTH_RTS);
    else
      LED_OFF(LED_BLUETOOTH_RTS);

  }

  if (PB(PB_TOGGLE_RX_BLUETOOTH_PC) && !bluetoothTogglePending)
    bluetoothTogglePending = 1; /* btn pushed */

  if (!PB(PB_TOGGLE_RX_BLUETOOTH_PC) && (bluetoothTogglePending == 2))
    bluetoothTogglePending = 0; /* toggle done & btn released */

  if ((bluetoothTogglePending == 1) &&
      CBUF_IsEmpty(pcTxBuffer) && CBUF_IsEmpty(pcRxBuffer) &&
      !(UCSR1B&_BV(UDRIE1))) {

    if (BLUETOOTH_ACTIVE) {
      PORTD &= ~_BV(BIT_ENABLE_RXD_LINK_BLUETOOTH);
      PORTD |= _BV(BIT_ENABLE_RXD_LINK_PC);
      LED_OFF(LED_BLUETOOTH_ACTIVE);
    } else {
      PORTD &= ~_BV(BIT_ENABLE_RXD_LINK_PC);
      PORTD |= _BV(BIT_ENABLE_RXD_LINK_BLUETOOTH);
      LED_ON(LED_BLUETOOTH_ACTIVE);
    }

    bluetoothTogglePending = 2;
  }

  if (BLUETOOTH_ACTIVE &&
      !CBUF_IsEmpty(pcTxBuffer) &&
      !(PORTD&_BV(BIT_BLUETOOTH_RTS)))
    UCSR1B |= _BV(UDRIE1);

#ifdef ENABLE_CHARGING

  if (PB(PB_TOGGLE_CHARGE_ENABLE) && !chargeEnableTogglePending)
    chargeEnableTogglePending = 1; /* btn pushed */

  if (!PB(PB_TOGGLE_CHARGE_ENABLE) && (chargeEnableTogglePending == 2))
    chargeEnableTogglePending = 0; /* toggle done & btn released */

  if (chargeEnableTogglePending == 1) {
    chargeEnabled = !chargeEnabled;
    chargeEnableTogglePending = 2;
  }

  if (chargePhaseTicksToGo) {

    chargePhaseTicksToGo--;

  } else if (chargePhase == CHARGE_PHASE) {

    chargePhase = MEASURE_PHASE;
    chargePhaseTicksToGo = MEASURE_PHASE_TICKS;

    if (charging) {
      PORTB |= _BV(BIT_CHARGE); /* stop charging */
      LED_OFF(LED_PWR);
    }

  } else {

    chargePhase = CHARGE_PHASE;
    chargePhaseTicksToGo = CHARGE_PHASE_TICKS;

    if (charging) {
      PORTB &= ~_BV(BIT_CHARGE); /* start charging */
      LED_ON(LED_PWR);
    }
  }

  if ((chargePhase == MEASURE_PHASE) &&
      (chargePhaseTicksToGo == MEASURE_TICKS)) {

    readADCs();

    if (charging &&
        (!chargeEnabled ||
         (adcValue[CHANNEL_NEG] < CHARGE_COMPLETE_THRESHOLD))) {

      charging = 0;
      chargeEnabled = 0;

      PORTB |= _BV(BIT_CHARGE); /* stop charging */

      LED_ON(LED_PWR);
    }

    if (chargeEnabled && (adcValue[CHANNEL_NEG] > CHARGE_START_THRESHOLD))
      charging = 1;
  }

#endif /* ENABLE_CHARGING */

}

/* default interrupt vector lights LEDs and hangs (should not get here) */
ISR(__vector_default) {

  LED_ON(LED_ALL);

  for (;;)
    ;
}

