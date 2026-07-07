package com.user.service;

import com.user.dto.OtpRequestDTO;
import com.user.dto.OtpVerifyRequestDTO;
import com.user.dto.ResponseDTO;
import com.user.dto.ResetPasswordRequestDTO;

public interface OtpService {

	ResponseDTO generateOtp(OtpRequestDTO otpRequestDTO);

	ResponseDTO verifyOtp(OtpVerifyRequestDTO otpVerifyRequestDTO);

}
